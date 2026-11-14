package com.mengyiding.wujiu.data.backup

import org.json.JSONArray
import org.json.JSONObject

data class BackupCategoryRecord(
    val name: String,
    val iconKey: String,
    val isBuiltIn: Boolean,
)

data class BackupAssetRecord(
    val name: String,
    val categoryName: String,
    val priceCents: Long,
    val purchaseDateEpochDay: Long,
    val notes: String,
    val inactiveDateEpochDay: Long?,
)

data class WujiuBackupData(
    val version: Int = WujiuBackupCodec.CURRENT_VERSION,
    val exportedAtMillis: Long,
    val categories: List<BackupCategoryRecord>,
    val assets: List<BackupAssetRecord>,
)

data class BackupImportResult(
    val importedAssets: Int,
    val skippedDuplicates: Int,
    val addedCategories: Int,
)

/**
 * Reflection-free JSON codec for offline backup/restore via org.json.
 */
object WujiuBackupCodec {
    const val CURRENT_VERSION = 1

    fun encode(data: WujiuBackupData): String {
        val categoriesJson = JSONArray()
        data.categories.forEach { category ->
            categoriesJson.put(
                JSONObject().apply {
                    put("name", category.name)
                    put("iconKey", category.iconKey)
                    put("isBuiltIn", category.isBuiltIn)
                },
            )
        }
        val assetsJson = JSONArray()
        data.assets.forEach { asset ->
            assetsJson.put(
                JSONObject().apply {
                    put("name", asset.name)
                    put("categoryName", asset.categoryName)
                    put("priceCents", asset.priceCents)
                    put("purchaseDateEpochDay", asset.purchaseDateEpochDay)
                    put("notes", asset.notes)
                    if (asset.inactiveDateEpochDay != null) {
                        put("inactiveDateEpochDay", asset.inactiveDateEpochDay)
                    } else {
                        put("inactiveDateEpochDay", JSONObject.NULL)
                    }
                },
            )
        }
        return JSONObject().apply {
            put("version", data.version)
            put("exportedAtMillis", data.exportedAtMillis)
            put("categories", categoriesJson)
            put("assets", assetsJson)
        }.toString(2)
    }

    fun decode(rawJson: String): WujiuBackupData {
        require(rawJson.isNotBlank()) { "备份文件为空" }
        val root = try {
            JSONObject(rawJson)
        } catch (ex: Exception) {
            throw IllegalArgumentException("备份文件不是有效的 JSON 格式", ex)
        }
        val version = root.optInt("version", -1)
        require(version in 1..CURRENT_VERSION) { "不支持的备份版本：$version" }
        val exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis())

        val categoriesArray = root.optJSONArray("categories")
            ?: throw IllegalArgumentException("备份缺少 categories 字段")
        val categories = buildList(categoriesArray.length()) {
            for (i in 0 until categoriesArray.length()) {
                val obj = categoriesArray.optJSONObject(i)
                    ?: throw IllegalArgumentException("分类条目格式无效 (#$i)")
                val name = obj.optString("name", "").trim()
                require(name.isNotEmpty() && name.length <= 20) { "备份包含非法分类名称 (#$i)" }
                val iconKey = obj.optString("iconKey", "other").ifBlank { "other" }
                val isBuiltIn = obj.optBoolean("isBuiltIn", false)
                add(BackupCategoryRecord(name = name, iconKey = iconKey, isBuiltIn = isBuiltIn))
            }
        }

        val assetsArray = root.optJSONArray("assets")
            ?: throw IllegalArgumentException("备份缺少 assets 字段")
        val assets = buildList(assetsArray.length()) {
            for (i in 0 until assetsArray.length()) {
                val obj = assetsArray.optJSONObject(i)
                    ?: throw IllegalArgumentException("物品条目格式无效 (#$i)")
                val name = obj.optString("name", "").trim()
                require(name.isNotEmpty() && name.length <= 40) { "备份包含非法物品名称 (#$i)" }
                val categoryName = obj.optString("categoryName", "").trim().ifEmpty { "其他" }
                require(obj.has("priceCents") && !obj.isNull("priceCents")) {
                    "物品缺少价格字段：$name"
                }
                val priceCents = obj.getLong("priceCents")
                require(priceCents > 0L) { "物品价格必须大于 0：$name" }
                require(obj.has("purchaseDateEpochDay") && !obj.isNull("purchaseDateEpochDay")) {
                    "物品缺少购买日期：$name"
                }
                val purchaseDateEpochDay = obj.getLong("purchaseDateEpochDay")
                val notes = obj.optString("notes", "").trim()
                require(notes.length <= 200) { "物品备注过长：$name" }
                val inactiveDateEpochDay = if (!obj.has("inactiveDateEpochDay") || obj.isNull("inactiveDateEpochDay")) {
                    null
                } else {
                    obj.getLong("inactiveDateEpochDay").also { inactive ->
                        require(inactive >= purchaseDateEpochDay) {
                            "停用日期不能早于购买日期：$name"
                        }
                    }
                }
                add(
                    BackupAssetRecord(
                        name = name,
                        categoryName = categoryName,
                        priceCents = priceCents,
                        purchaseDateEpochDay = purchaseDateEpochDay,
                        notes = notes,
                        inactiveDateEpochDay = inactiveDateEpochDay,
                    ),
                )
            }
        }

        return WujiuBackupData(
            version = version,
            exportedAtMillis = exportedAtMillis,
            categories = categories,
            assets = assets,
        )
    }
}
