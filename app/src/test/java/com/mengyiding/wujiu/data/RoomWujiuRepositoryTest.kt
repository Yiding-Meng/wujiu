package com.mengyiding.wujiu.data

import androidx.test.core.app.ApplicationProvider
import com.mengyiding.wujiu.data.local.WujiuDatabase
import com.mengyiding.wujiu.data.repository.NewAsset
import com.mengyiding.wujiu.data.repository.RoomWujiuRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomWujiuRepositoryTest {
    private lateinit var database: WujiuDatabase
    private val deletedImages = mutableListOf<String>()
    private lateinit var repository: RoomWujiuRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        database = WujiuDatabase.createInMemory(context)
        deletedImages.clear()
        repository = RoomWujiuRepository(
            database = database,
            onDeleteImage = { path -> deletedImages += path },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `deleteAsset removes row and cleans up local image after commit`() = runTest {
        val categories = repository.observeCategories().first()
        val phoneId = categories.first { it.name == "手机" }.id
        val id = repository.addAsset(
            NewAsset(
                name = "测试手机",
                categoryId = phoneId,
                priceCents = 499_900L,
                purchaseDateEpochDay = 20_000L,
                notes = "",
                imagePath = "/tmp/asset_1.jpg",
                inactiveDateEpochDay = null,
            ),
        )

        repository.deleteAsset(id)

        assertEquals(emptyList<Any>(), repository.observeAssets().first())
        assertEquals(listOf("/tmp/asset_1.jpg"), deletedImages)
    }

    @Test
    fun `updateAsset deletes replaced image and preserves same image`() = runTest {
        val categories = repository.observeCategories().first()
        val phoneId = categories.first { it.name == "手机" }.id
        val id = repository.addAsset(
            NewAsset(
                name = "相机",
                categoryId = phoneId,
                priceCents = 800_000L,
                purchaseDateEpochDay = 20_000L,
                notes = "",
                imagePath = "/tmp/old.jpg",
                inactiveDateEpochDay = null,
            ),
        )

        repository.updateAsset(
            id,
            NewAsset(
                name = "相机 II",
                categoryId = phoneId,
                priceCents = 850_000L,
                purchaseDateEpochDay = 20_000L,
                notes = "升级",
                imagePath = "/tmp/old.jpg",
                inactiveDateEpochDay = null,
            ),
        )
        assertEquals(emptyList<String>(), deletedImages)

        repository.updateAsset(
            id,
            NewAsset(
                name = "相机 II",
                categoryId = phoneId,
                priceCents = 850_000L,
                purchaseDateEpochDay = 20_000L,
                notes = "换图",
                imagePath = "/tmp/new.jpg",
                inactiveDateEpochDay = null,
            ),
        )
        assertEquals(listOf("/tmp/old.jpg"), deletedImages)
    }

    @Test
    fun `missing asset operations fail explicitly instead of silently succeeding`() = runTest {
        val categories = repository.observeCategories().first()
        val otherId = categories.first { it.name == "其他" }.id
        val dummy = NewAsset("Ghost", otherId, 100L, 20_000L, "", null, null)

        try {
            repository.updateAsset(9999L, dummy)
            fail("Expected NoSuchElementException")
        } catch (_: NoSuchElementException) {
        }
        try {
            repository.setInactive(9999L, 20_001L)
            fail("Expected NoSuchElementException")
        } catch (_: NoSuchElementException) {
        }
        try {
            repository.deleteAsset(9999L)
            fail("Expected NoSuchElementException")
        } catch (_: NoSuchElementException) {
        }
    }

    @Test
    fun `addAsset validates category existence and setInactive rejects early date`() = runTest {
        try {
            repository.addAsset(NewAsset("Invalid", 99_999L, 100L, 20_000L, "", null, null))
            fail("Expected IllegalArgumentException for deleted category")
        } catch (_: IllegalArgumentException) {
        }

        val otherId = repository.observeCategories().first().first { it.name == "其他" }.id
        val id = repository.addAsset(NewAsset("键盘", otherId, 50_000L, 20_010L, "", null, null))
        try {
            repository.setInactive(id, 20_005L)
            fail("Expected IllegalArgumentException for inactive date before purchase date")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `export and import backup supports idempotent merge and transactional replace`() = runTest {
        val customId = repository.addCategory("露营")
        repository.addAsset(
            NewAsset(
                name = "账篷",
                categoryId = customId,
                priceCents = 120_000L,
                purchaseDateEpochDay = 20_000L,
                notes = "轻量",
                imagePath = "/tmp/tent.jpg",
                inactiveDateEpochDay = null,
            ),
        )
        val backup = repository.exportBackup()

        val mergeSame = repository.importBackup(backup, replaceExisting = false)
        assertEquals(0, mergeSame.importedAssets)
        assertEquals(1, mergeSame.skippedDuplicates)
        assertEquals(1, repository.observeAssets().first().size)

        val replace = repository.importBackup(backup, replaceExisting = true)
        assertEquals(1, replace.importedAssets)
        assertEquals(listOf("/tmp/tent.jpg"), deletedImages)
        val restored = repository.observeAssets().first()
        assertEquals(1, restored.size)
        assertEquals("露营", restored.first().category.name)
        assertFalse(restored.first().category.isBuiltIn)
        assertTrue(restored.first().asset.imagePath == null)
    }
}
