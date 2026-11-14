package com.mengyiding.wujiu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mengyiding.wujiu.data.backup.WujiuBackupCodec
import com.mengyiding.wujiu.data.backup.WujiuBackupData
import com.mengyiding.wujiu.data.repository.WujiuRepository
import com.mengyiding.wujiu.domain.calculation.CostCalculator
import com.mengyiding.wujiu.util.todayLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class HomeFeedbackState(
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val pendingImport: WujiuBackupData? = null,
)

class HomeViewModel(
    private val repository: WujiuRepository,
) : ViewModel() {
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val feedback = MutableStateFlow(HomeFeedbackState())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeAssets(),
        repository.observeCategories(),
        selectedCategoryId,
        feedback,
    ) { assets, categories, selectedId, currentFeedback ->
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
        val todayCost = CostCalculator.todayActiveCostCents(
            items = assets.map {
                Triple(
                    it.asset.priceCents,
                    it.asset.purchaseDateEpochDay,
                    it.asset.inactiveDateEpochDay,
                )
            },
            todayEpochDay = today,
        )
        HomeUiState(
            categories = categories,
            assets = cards.sortedByDescending { it.dailyCost },
            selectedCategoryId = selectedId,
            totalSpendCents = assets.sumOf { it.asset.priceCents },
            todayCost = todayCost,
            activeCount = activeAssetCount,
            isLoading = false,
            errorMessage = currentFeedback.errorMessage,
            statusMessage = currentFeedback.statusMessage,
            pendingImport = currentFeedback.pendingImport,
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

    suspend fun createBackupJson(): String {
        val data = repository.exportBackup()
        return WujiuBackupCodec.encode(data)
    }

    fun onBackupExported() {
        feedback.update { it.copy(statusMessage = "数据备份已导出") }
    }

    fun prepareImportJson(rawJson: String) {
        runCatching { WujiuBackupCodec.decode(rawJson) }
            .onSuccess { decoded ->
                feedback.update { it.copy(pendingImport = decoded, errorMessage = null) }
            }
            .onFailure { throwable ->
                feedback.update {
                    it.copy(errorMessage = throwable.message ?: "无法解析备份文件")
                }
            }
    }

    fun dismissPendingImport() {
        feedback.update { it.copy(pendingImport = null) }
    }

    fun confirmImport(replaceExisting: Boolean) {
        val backup = feedback.value.pendingImport ?: return
        feedback.update { it.copy(pendingImport = null) }
        viewModelScope.launch {
            runCatching { repository.importBackup(backup, replaceExisting = replaceExisting) }
                .onSuccess { result ->
                    val modeText = if (replaceExisting) "已覆盖恢复" else "已合并导入"
                    val skippedText = if (result.skippedDuplicates > 0) {
                        "，跳过 ${result.skippedDuplicates} 条重复记录"
                    } else {
                        ""
                    }
                    feedback.update {
                        it.copy(statusMessage = "${modeText} ${result.importedAssets} 件物品$skippedText")
                    }
                }
                .onFailure { throwable ->
                    feedback.update {
                        it.copy(errorMessage = throwable.message ?: "恢复备份失败")
                    }
                }
        }
    }

    fun notifyError(message: String) {
        feedback.update { it.copy(errorMessage = message) }
    }

    fun dismissError() {
        feedback.update { it.copy(errorMessage = null) }
    }

    fun dismissStatusMessage() {
        feedback.update { it.copy(statusMessage = null) }
    }
}
