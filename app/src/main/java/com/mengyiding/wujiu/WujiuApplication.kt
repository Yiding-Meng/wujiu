package com.mengyiding.wujiu

import android.app.Application
import com.mengyiding.wujiu.data.image.LocalImageStore
import com.mengyiding.wujiu.data.local.WujiuDatabase
import com.mengyiding.wujiu.data.repository.RoomWujiuRepository
import com.mengyiding.wujiu.data.repository.WujiuRepository

class WujiuApplication : Application() {
    val imageStore: LocalImageStore by lazy { LocalImageStore(this) }

    val repository: WujiuRepository by lazy {
        val database = WujiuDatabase.getInstance(this)
        RoomWujiuRepository(database.categoryDao(), database.assetDao())
    }
}
