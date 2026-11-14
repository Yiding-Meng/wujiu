package com.mengyiding.wujiu.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mengyiding.wujiu.data.repository.WujiuRepository
import com.mengyiding.wujiu.domain.calculation.CostCalculator
import com.mengyiding.wujiu.util.todayLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: WujiuRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val assetId: Long = checkNotNull(savedStateHandle["assetId"])
    private val localError = MutableStateFlow<String?>(null)
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeAsset(assetId),
        localError,
    ) { item, error ->
        if (item == null) {
            DetailUiState(isLoading = false, notFound = true, errorMessage = error)
        } else {
            val today = todayLocalDate().toEpochDay()
            val days = CostCalculator.usageDays(
                item.asset.purchaseDateEpochDay,
                item.asset.inactiveDateEpochDay,
                today,
            )
            DetailUiState(
                isLoading = false,
                id = item.asset.id,
                name = item.asset.name,
                category = item.category,
                priceCents = item.asset.priceCents,
                purchaseDateEpochDay = item.asset.purchaseDateEpochDay,
                inactiveDateEpochDay = item.asset.inactiveDateEpochDay,
                usageDays = days,
                dailyCost = CostCalculator.dailyCostCents(item.asset.priceCents, days),
                notes = item.asset.notes,
                imagePath = item.asset.imagePath,
                errorMessage = error,
            )
        }
    }
        .catch { throwable ->
            emit(DetailUiState(isLoading = false, errorMessage = throwable.message ?: "读取物品详情失败"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState(),
        )

    fun stopUsing(dateEpochDay: Long) {
        val state = uiState.value
        val today = todayLocalDate().toEpochDay()
        if (!state.isLoading && !state.notFound) {
            if (dateEpochDay < state.purchaseDateEpochDay) {
                localError.value = "停用日期不能早于购买日期"
                return
            }
            if (dateEpochDay > today) {
                localError.value = "停用日期不能晚于今天"
                return
            }
        }
        viewModelScope.launch {
            runCatching { repository.setInactive(assetId, dateEpochDay) }
                .onFailure { localError.value = it.message ?: "操作失败，请重试" }
        }
    }

    fun resumeUsing() {
        viewModelScope.launch {
            runCatching { repository.setInactive(assetId, null) }
                .onFailure { localError.value = it.message ?: "操作失败，请重试" }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        if (_deleted.value) return
        viewModelScope.launch {
            runCatching { repository.deleteAsset(assetId) }
                .onSuccess {
                    _deleted.value = true
                    onDeleted()
                }
                .onFailure { localError.value = it.message ?: "删除失败，请重试" }
        }
    }

    fun dismissError() {
        localError.value = null
    }
}
