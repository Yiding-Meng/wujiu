package com.mengyiding.wujiu.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Transaction
    @Query("SELECT * FROM assets ORDER BY id DESC")
    fun observeAllRows(): Flow<List<AssetWithCategoryRow>>

    @Transaction
    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    fun observeOneWithCategory(id: Long): Flow<AssetWithCategoryRow?>

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): AssetEntity?

    @Query("SELECT COUNT(*) FROM assets WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(asset: AssetEntity): Long

    @Update
    suspend fun update(asset: AssetEntity)

    @Delete
    suspend fun delete(asset: AssetEntity)
}
