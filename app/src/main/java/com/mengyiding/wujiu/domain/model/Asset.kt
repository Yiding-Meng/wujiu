package com.mengyiding.wujiu.domain.model

data class Asset(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val priceCents: Long,
    val purchaseDateEpochDay: Long,
    val notes: String,
    val imagePath: String?,
    val inactiveDateEpochDay: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class AssetWithCategory(
    val asset: Asset,
    val category: Category,
)
