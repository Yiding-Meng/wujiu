package com.mengyiding.wujiu.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

internal val defaultCategories = listOf(
    Triple("手机", "phone", 1L),
    Triple("电脑", "computer", 2L),
    Triple("相机", "camera", 3L),
    Triple("家电", "appliance", 4L),
    Triple("交通", "transport", 5L),
    Triple("穿戴", "wearable", 6L),
    Triple("影音", "audio", 7L),
    Triple("其他", "other", 8L),
)

internal object DefaultDataCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val now = System.currentTimeMillis()
        defaultCategories.forEach { (name, icon, order) ->
            db.execSQL(
                "INSERT INTO categories (name, iconKey, isBuiltIn, createdAt) VALUES (?, ?, 1, ?)",
                arrayOf<Any>(name, icon, now + order),
            )
        }
    }
}
