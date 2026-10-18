package com.mengyiding.wujiu.data.repository

import com.mengyiding.wujiu.domain.model.Asset
import com.mengyiding.wujiu.domain.model.AssetWithCategory
import com.mengyiding.wujiu.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface WujiuRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeAssets(): Flow<List<AssetWithCategory>>
    fun observeAsset(id: Long): Flow<AssetWithCategory?>
    suspend fun getAsset(id: Long): Asset?
    suspend fun deleteCategoryIfUnused(id: Long): Boolean
    suspend fun hasAssetsInCategory(categoryId: Long): Boolean
    suspend fun addAsset(asset: NewAsset): Long
    suspend fun updateAsset(id: Long, asset: NewAsset)
    suspend fun setInactive(id: Long, inactiveEpochDay: Long?)
    suspend fun deleteAsset(id: Long)
    suspend fun addCategory(name: String): Long
}

data class NewAsset(
    val name: String,
    val categoryId: Long,
    val priceCents: Long,
    val purchaseDateEpochDay: Long,
    val notes: String,
    val imagePath: String?,
    val inactiveDateEpochDay: Long?,
)
