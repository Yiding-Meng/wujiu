package com.mengyiding.wujiu.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class AssetWithCategoryRow(
    @Embedded val asset: AssetEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity,
)
