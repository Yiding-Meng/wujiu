package com.mengyiding.wujiu.data.repository

import com.mengyiding.wujiu.data.local.AssetDao
import com.mengyiding.wujiu.data.local.AssetEntity
import com.mengyiding.wujiu.data.local.AssetWithCategoryRow
import com.mengyiding.wujiu.data.local.CategoryDao
import com.mengyiding.wujiu.data.local.CategoryEntity
import com.mengyiding.wujiu.domain.model.Asset
import com.mengyiding.wujiu.domain.model.AssetWithCategory
import com.mengyiding.wujiu.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWujiuRepository(
    private val categoryDao: CategoryDao,
    private val assetDao: AssetDao,
) : WujiuRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { rows -> rows.map(CategoryEntity::toDomain) }

    override fun observeAssets(): Flow<List<AssetWithCategory>> =
        assetDao.observeAllRows().map { rows ->
            rows.map(AssetWithCategoryRow::toDomain)
                .sortedBy { it.asset.purchaseDateEpochDay }
                .reversed()
        }

    override fun observeAsset(id: Long): Flow<AssetWithCategory?> =
        assetDao.observeOneWithCategory(id).map { it?.toDomain() }

    override suspend fun getAsset(id: Long): Asset? = assetDao.findById(id)?.toDomain()

    override suspend fun hasAssetsInCategory(categoryId: Long): Boolean =
        assetDao.countByCategory(categoryId) > 0

    override suspend fun addAsset(asset: NewAsset): Long {
        val now = System.currentTimeMillis()
        return assetDao.insert(
            AssetEntity(
                name = asset.name.trim(),
                categoryId = asset.categoryId,
                priceCents = asset.priceCents,
                purchaseDateEpochDay = asset.purchaseDateEpochDay,
                notes = asset.notes.trim(),
                imagePath = asset.imagePath,
                inactiveDateEpochDay = asset.inactiveDateEpochDay,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun updateAsset(id: Long, asset: NewAsset) {
        val current = assetDao.findById(id) ?: return
        assetDao.update(
            current.copy(
                name = asset.name.trim(),
                categoryId = asset.categoryId,
                priceCents = asset.priceCents,
                purchaseDateEpochDay = asset.purchaseDateEpochDay,
                notes = asset.notes.trim(),
                imagePath = asset.imagePath,
                inactiveDateEpochDay = asset.inactiveDateEpochDay,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun setInactive(id: Long, inactiveEpochDay: Long?) {
        val current = assetDao.findById(id) ?: return
        assetDao.update(
            current.copy(
                inactiveDateEpochDay = inactiveEpochDay,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteAsset(id: Long) {
        assetDao.findById(id)?.let { assetDao.delete(it) }
    }

    override suspend fun addCategory(name: String): Long {
        val normalized = name.trim()
        if (normalized.isEmpty()) {
            throw IllegalArgumentException("分类名称不能为空")
        }
        if (categoryDao.findByName(normalized) != null) {
            throw IllegalArgumentException("分类名称已存在")
        }
        return categoryDao.insert(
            CategoryEntity(
                name = normalized,
                iconKey = "other",
                isBuiltIn = false,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun deleteCategoryIfUnused(id: Long): Boolean =
        categoryDao.deleteIfUnused(id) > 0
}

private fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    iconKey = iconKey,
    isBuiltIn = isBuiltIn,
)

private fun AssetEntity.toDomain() = Asset(
    id = id,
    name = name,
    categoryId = categoryId,
    priceCents = priceCents,
    purchaseDateEpochDay = purchaseDateEpochDay,
    notes = notes,
    imagePath = imagePath,
    inactiveDateEpochDay = inactiveDateEpochDay,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun AssetWithCategoryRow.toDomain() = AssetWithCategory(
    asset = asset.toDomain(),
    category = category.toDomain(),
)
