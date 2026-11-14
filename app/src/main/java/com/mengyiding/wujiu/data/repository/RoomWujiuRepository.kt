package com.mengyiding.wujiu.data.repository

import androidx.room.withTransaction
import com.mengyiding.wujiu.data.backup.BackupAssetRecord
import com.mengyiding.wujiu.data.backup.BackupCategoryRecord
import com.mengyiding.wujiu.data.backup.BackupImportResult
import com.mengyiding.wujiu.data.backup.WujiuBackupCodec
import com.mengyiding.wujiu.data.backup.WujiuBackupData
import com.mengyiding.wujiu.data.local.AssetEntity
import com.mengyiding.wujiu.data.local.AssetWithCategoryRow
import com.mengyiding.wujiu.data.local.CategoryEntity
import com.mengyiding.wujiu.data.local.WujiuDatabase
import com.mengyiding.wujiu.data.local.defaultCategories
import com.mengyiding.wujiu.domain.model.Asset
import com.mengyiding.wujiu.domain.model.AssetWithCategory
import com.mengyiding.wujiu.domain.model.Category
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomWujiuRepository(
    private val database: WujiuDatabase,
    private val onDeleteImage: (String) -> Unit = {},
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WujiuRepository {
    private val categoryDao = database.categoryDao()
    private val assetDao = database.assetDao()

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
        validateNewAsset(asset)
        return database.withTransaction {
            requireCategoryExists(asset.categoryId)
            val now = System.currentTimeMillis()
            assetDao.insert(
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
    }

    override suspend fun updateAsset(id: Long, asset: NewAsset) {
        validateNewAsset(asset)
        val obsoleteImage = database.withTransaction {
            val current = assetDao.findById(id)
                ?: throw NoSuchElementException("物品不存在或已被删除")
            requireCategoryExists(asset.categoryId)
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
            current.imagePath?.takeIf { it != asset.imagePath }
        }
        deleteImageSafely(obsoleteImage)
    }

    override suspend fun setInactive(id: Long, inactiveEpochDay: Long?) {
        database.withTransaction {
            val current = assetDao.findById(id)
                ?: throw NoSuchElementException("物品不存在或已被删除")
            if (inactiveEpochDay != null && inactiveEpochDay < current.purchaseDateEpochDay) {
                throw IllegalArgumentException("停用日期不能早于购买日期")
            }
            assetDao.update(
                current.copy(
                    inactiveDateEpochDay = inactiveEpochDay,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun deleteAsset(id: Long) {
        val deletedImage = database.withTransaction {
            val current = assetDao.findById(id)
                ?: throw NoSuchElementException("物品不存在或已被删除")
            assetDao.delete(current)
            current.imagePath
        }
        deleteImageSafely(deletedImage)
    }

    override suspend fun addCategory(name: String): Long {
        val normalized = name.trim()
        if (normalized.isEmpty()) {
            throw IllegalArgumentException("分类名称不能为空")
        }
        if (normalized.length > 20) {
            throw IllegalArgumentException("分类名称不能超过 20 个字")
        }
        return database.withTransaction {
            if (categoryDao.findByName(normalized) != null) {
                throw IllegalArgumentException("分类名称已存在")
            }
            categoryDao.insert(
                CategoryEntity(
                    name = normalized,
                    iconKey = "other",
                    isBuiltIn = false,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun deleteCategoryIfUnused(id: Long): Boolean =
        database.withTransaction {
            categoryDao.deleteIfUnused(id) > 0
        }

    override suspend fun exportBackup(): WujiuBackupData = database.withTransaction {
        val categories = categoryDao.observeAll().first()
        val categoryNamesById = categories.associate { it.id to it.name }
        val assets = assetDao.listAll().map { row ->
            BackupAssetRecord(
                name = row.name,
                categoryName = categoryNamesById[row.categoryId] ?: "其他",
                priceCents = row.priceCents,
                purchaseDateEpochDay = row.purchaseDateEpochDay,
                notes = row.notes,
                inactiveDateEpochDay = row.inactiveDateEpochDay,
            )
        }
        WujiuBackupData(
            version = WujiuBackupCodec.CURRENT_VERSION,
            exportedAtMillis = System.currentTimeMillis(),
            categories = categories.map {
                BackupCategoryRecord(
                    name = it.name,
                    iconKey = it.iconKey,
                    isBuiltIn = it.isBuiltIn,
                )
            },
            assets = assets,
        )
    }

    override suspend fun importBackup(
        backup: WujiuBackupData,
        replaceExisting: Boolean,
    ): BackupImportResult {
        backup.assets.forEach { asset ->
            require(asset.name.trim().isNotEmpty() && asset.name.trim().length <= 40) {
                "备份包含无效物品名称：${asset.name}"
            }
            require(asset.priceCents > 0L) { "备份包含无效价格：${asset.name}" }
            require(asset.inactiveDateEpochDay == null || asset.inactiveDateEpochDay >= asset.purchaseDateEpochDay) {
                "停用日期不能早于购买日期：${asset.name}"
            }
        }

        val obsoleteImages = mutableListOf<String>()
        val result = database.withTransaction {
            val now = System.currentTimeMillis()
            if (replaceExisting) {
                assetDao.listAll().mapNotNullTo(obsoleteImages) { it.imagePath }
                assetDao.deleteAll()
                categoryDao.observeAll().first()
                    .filter { !it.isBuiltIn }
                    .forEach { categoryDao.deleteIfUnused(it.id) }
            }

            val existingCategories = categoryDao.observeAll().first()
                .associateBy { it.name }
                .toMutableMap()

            if (existingCategories.isEmpty()) {
                defaultCategories.forEach { (seedName, seedIcon, seedOrder) ->
                    val id = categoryDao.insert(
                        CategoryEntity(
                            name = seedName,
                            iconKey = seedIcon,
                            isBuiltIn = true,
                            createdAt = now + seedOrder,
                        ),
                    )
                    existingCategories[seedName] = CategoryEntity(
                        id = id,
                        name = seedName,
                        iconKey = seedIcon,
                        isBuiltIn = true,
                        createdAt = now + seedOrder,
                    )
                }
            }

            var addedCategories = 0
            backup.categories.forEach { rec ->
                val catName = rec.name.trim()
                if (catName.isNotEmpty() && catName !in existingCategories) {
                    val safeIcon = rec.iconKey.takeIf { it in ALLOWED_ICON_KEYS } ?: "other"
                    val id = categoryDao.insert(
                        CategoryEntity(
                            name = catName.take(20),
                            iconKey = safeIcon,
                            isBuiltIn = false,
                            createdAt = now,
                        ),
                    )
                    existingCategories[catName] = CategoryEntity(
                        id = id,
                        name = catName.take(20),
                        iconKey = safeIcon,
                        isBuiltIn = false,
                        createdAt = now,
                    )
                    addedCategories += 1
                }
            }

            val existingSignatures = if (replaceExisting) {
                emptySet()
            } else {
                assetDao.listAll().mapTo(HashSet()) { it.signature() }
            }

            var importedAssets = 0
            var skippedDuplicates = 0
            backup.assets.forEachIndexed { index, rec ->
                val trimmedName = rec.name.trim()
                val categoryName = rec.categoryName.trim().ifEmpty { "其他" }
                val category = existingCategories[categoryName]
                    ?: existingCategories["其他"]
                    ?: run {
                        val safeIcon = "other"
                        val id = categoryDao.insert(
                            CategoryEntity(
                                name = categoryName.take(20),
                                iconKey = safeIcon,
                                isBuiltIn = false,
                                createdAt = now,
                            ),
                        )
                        CategoryEntity(
                            id = id,
                            name = categoryName.take(20),
                            iconKey = safeIcon,
                            isBuiltIn = false,
                            createdAt = now,
                        ).also {
                            existingCategories[categoryName] = it
                            addedCategories += 1
                        }
                    }

                val trimmedNotes = rec.notes.trim().take(200)
                val sig = AssetSignature(
                    name = trimmedName,
                    categoryId = category.id,
                    priceCents = rec.priceCents,
                    purchaseDateEpochDay = rec.purchaseDateEpochDay,
                    notes = trimmedNotes,
                    inactiveDateEpochDay = rec.inactiveDateEpochDay,
                )
                if (!replaceExisting && sig in existingSignatures) {
                    skippedDuplicates += 1
                } else {
                    val ts = now + index
                    assetDao.insert(
                        AssetEntity(
                            name = trimmedName,
                            categoryId = category.id,
                            priceCents = rec.priceCents,
                            purchaseDateEpochDay = rec.purchaseDateEpochDay,
                            notes = trimmedNotes,
                            imagePath = null,
                            inactiveDateEpochDay = rec.inactiveDateEpochDay,
                            createdAt = ts,
                            updatedAt = ts,
                        ),
                    )
                    importedAssets += 1
                }
            }

            BackupImportResult(
                importedAssets = importedAssets,
                skippedDuplicates = skippedDuplicates,
                addedCategories = addedCategories,
            )
        }
        obsoleteImages.forEach { deleteImageSafely(it) }
        return result
    }

    private fun validateNewAsset(asset: NewAsset) {
        val trimmedName = asset.name.trim()
        require(trimmedName.isNotEmpty()) { "物品名称不能为空" }
        require(trimmedName.length <= 40) { "物品名称不能超过 40 个字" }
        require(asset.priceCents > 0L) { "购买价格必须大于 0" }
        require(asset.notes.trim().length <= 200) { "备注不能超过 200 个字" }
        require(asset.inactiveDateEpochDay == null || asset.inactiveDateEpochDay >= asset.purchaseDateEpochDay) {
            "停用日期不能早于购买日期"
        }
    }

    private suspend fun requireCategoryExists(categoryId: Long) {
        if (categoryDao.findById(categoryId) == null) {
            throw IllegalArgumentException("所选分类不存在或已被删除，请重新选择")
        }
    }

    private suspend fun deleteImageSafely(path: String?) {
        if (path.isNullOrBlank()) return
        withContext(NonCancellable + ioDispatcher) {
            runCatching { onDeleteImage(path) }
        }
    }

    private companion object {
        val ALLOWED_ICON_KEYS: Set<String> = defaultCategories.mapTo(HashSet()) { it.second }
    }
}

private data class AssetSignature(
    val name: String,
    val categoryId: Long,
    val priceCents: Long,
    val purchaseDateEpochDay: Long,
    val notes: String,
    val inactiveDateEpochDay: Long?,
)

private fun AssetEntity.signature() = AssetSignature(
    name = name,
    categoryId = categoryId,
    priceCents = priceCents,
    purchaseDateEpochDay = purchaseDateEpochDay,
    notes = notes,
    inactiveDateEpochDay = inactiveDateEpochDay,
)

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
