package com.mengyiding.wujiu.ui.home

import com.mengyiding.wujiu.domain.model.Category
import java.math.BigDecimal

data class AssetCardModel(
    val id: Long,
    val name: String,
    val category: Category,
    val priceCents: Long,
    val usageDays: Long,
    val dailyCost: BigDecimal,
    val imagePath: String?,
    val isActive: Boolean,
)

data class HomeUiState(
    val categories: List<Category> = emptyList(),
    val assets: List<AssetCardModel> = emptyList(),
    val selectedCategoryId: Long? = null,
    val totalSpendCents: Long = 0,
    val todayCost: BigDecimal = BigDecimal.ZERO,
    val activeCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val filteredAssets: List<AssetCardModel>
        get() = if (selectedCategoryId == null) {
            assets
        } else {
            assets.filter { it.category.id == selectedCategoryId }
        }

    val averageDays: Long
        get() = if (assets.isEmpty()) 0 else assets.sumOf { it.usageDays } / assets.size
}
