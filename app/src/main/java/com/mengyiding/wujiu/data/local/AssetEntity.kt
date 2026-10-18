package com.mengyiding.wujiu.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId"), Index("purchaseDateEpochDay")],
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
