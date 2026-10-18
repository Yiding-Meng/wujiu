package com.mengyiding.wujiu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isBuiltIn DESC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Query(
        """
        DELETE FROM categories
        WHERE id = :id
          AND isBuiltIn = 0
          AND NOT EXISTS (SELECT 1 FROM assets WHERE categoryId = :id)
        """,
    )
    suspend fun deleteIfUnused(id: Long): Int
}
