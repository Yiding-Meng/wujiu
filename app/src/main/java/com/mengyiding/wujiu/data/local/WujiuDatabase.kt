package com.mengyiding.wujiu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CategoryEntity::class, AssetEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WujiuDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun assetDao(): AssetDao

    companion object {
        @Volatile
        private var instance: WujiuDatabase? = null

        fun getInstance(context: Context): WujiuDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WujiuDatabase::class.java,
                    "wujiu.db",
                )
                    .addCallback(DefaultDataCallback)
                    .build()
                    .also { instance = it }
            }
    }
}
