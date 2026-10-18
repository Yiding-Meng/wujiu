package com.mengyiding.wujiu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mengyiding.wujiu.data.repository.WujiuRepository
import com.mengyiding.wujiu.domain.calculation.CostCalculator
import com.mengyiding.wujiu.util.todayLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal

class HomeViewModel(
    private val repository: WujiuRepository,
) : ViewModel() {
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeAssets(),
        repository.observeCategories(),
        selectedCategoryId,
        errorMessage,
    ) { assets, categories, selectedId, error ->
        val today = todayLocalDate().toEpochDay()
        val cards = assets.map { item ->
            val asset = item.asset
            val days = CostCalculator.usageDays(
                purchaseEpochDay = asset.purchaseDateEpochDay,
                inactiveEpochDay = asset.inactiveDateEpochDay,
                todayEpochDay = today,
            )
            AssetCardModel(
                id = asset.id,
                name = asset.name,
                category = item.category,
                priceCents = asset.priceCents,
                usageDays = days,
                dailyCost = CostCalculator.dailyCostCents(asset.priceCents, days),
                imagePath = asset.imagePath,
                isActive = asset.inactiveDateEpochDay == null,
            )
        }
        val activeAssetCount = assets.count { it.asset.inactiveDateEpochDay == null }
        HomeUiState(
            categories = categories,
            assets = cards.sortedByDescending { it.dailyCost },
            selectedCategoryId = selectedId,
            totalSpendCents = assets.sumOf { it.asset.priceCents },
            todayCost = assets
                .filter { it.asset.inactiveDateEpochDay == null }
                .fold(BigDecimal.ZERO) { total, item ->
                    total + CostCalculator.dailyCostCents(
                        item.asset.priceCents,
                        CostCalculator.usageDays(
                            item.asset.purchaseDateEpochDay,
                            null,
                            today,
                        ),
                    )
                },
            activeCount = activeAssetCount,
            isLoading = false,
            errorMessage = error,
        )
    }.catch { throwable ->
        emit(
            HomeUiState(
                isLoading = false,
                errorMessage = throwable.message ?: "读取数据失败",
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun selectCategory(categoryId: Long?) {
        selectedCategoryId.value = categoryId
    }

    fun dismissError() {
        errorMessage.value = null
    }
}
