package com.mengyiding.wujiu.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.mengyiding.wujiu.data.backup.BackupImportResult
import com.mengyiding.wujiu.data.backup.WujiuBackupCodec
import com.mengyiding.wujiu.data.backup.WujiuBackupData
import com.mengyiding.wujiu.data.image.LocalImageStore
import com.mengyiding.wujiu.data.repository.NewAsset
import com.mengyiding.wujiu.data.repository.WujiuRepository
import com.mengyiding.wujiu.domain.calculation.CostCalculator
import com.mengyiding.wujiu.domain.model.Asset
import com.mengyiding.wujiu.domain.model.AssetWithCategory
import com.mengyiding.wujiu.domain.model.Category
import com.mengyiding.wujiu.ui.detail.DetailViewModel
import com.mengyiding.wujiu.ui.editor.EditorViewModel
import com.mengyiding.wujiu.ui.home.HomeViewModel
import com.mengyiding.wujiu.util.todayLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewModelsTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `homeViewModel uses CostCalculator and handles backup import state`() = runTest(testDispatcher) {
        val repo = FakeWujiuRepository()
        val today = todayLocalDate().toEpochDay()
        repo.seedAssets(
            AssetWithCategory(
                Asset(1L, "Laptop", 1L, 100_000L, today - 9, "", null, null, 1L, 1L),
                repo.categories.value[0],
            ),
            AssetWithCategory(
                Asset(2L, "OldPhone", 1L, 50_000L, today - 9, "", null, today - 2, 1L, 1L),
                repo.categories.value[0],
            ),
        )
        val vm = HomeViewModel(repo)
        val collector = launch { vm.uiState.collect() }
        advanceUntilIdle()

        val expectedCost = CostCalculator.todayActiveCostCents(
            listOf(Triple(100_000L, today - 9, null), Triple(50_000L, today - 9, today - 2)),
            today,
        )
        assertEquals(expectedCost, vm.uiState.value.todayCost)

        vm.prepareImportJson("bad-json")
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)
        vm.dismissError()
        advanceUntilIdle()
        assertNull(vm.uiState.value.errorMessage)

        val validJson = WujiuBackupCodec.encode(
            WujiuBackupData(1, 1000L, emptyList(), emptyList()),
        )
        vm.prepareImportJson(validJson)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.pendingImport)

        vm.confirmImport(replaceExisting = false)
        advanceUntilIdle()
        assertNull(vm.uiState.value.pendingImport)
        assertTrue(vm.uiState.value.statusMessage?.contains("已合并导入") == true)
        collector.cancel()
    }

    @Test
    fun `detailViewModel surfaces localError on invalid stopUsing date and repository errors`() = runTest(testDispatcher) {
        val repo = FakeWujiuRepository()
        val today = todayLocalDate().toEpochDay()
        repo.seedAssets(
            AssetWithCategory(
                Asset(10L, "Monitor", 1L, 300_000L, today - 5, "", null, null, 1L, 1L),
                repo.categories.value[0],
            ),
        )
        val vm = DetailViewModel(repo, SavedStateHandle(mapOf("assetId" to 10L)))
        val collector = launch { vm.uiState.collect() }
        advanceUntilIdle()

        vm.stopUsing(today - 10)
        advanceUntilIdle()
        assertEquals("停用日期不能早于购买日期", vm.uiState.value.errorMessage)
        vm.dismissError()
        advanceUntilIdle()
        assertNull(vm.uiState.value.errorMessage)

        repo.failOnDelete = IllegalStateException("删除失败")
        var deletedCallback = false
        vm.delete { deletedCallback = true }
        advanceUntilIdle()
        assertFalse(deletedCallback)
        assertEquals("删除失败", vm.uiState.value.errorMessage)
        collector.cancel()
    }

    @Test
    fun `editorViewModel falls back when selected custom category is deleted, caps price, and cleans unsaved image onCleared`() =
        runTest(testDispatcher) {
            val context = ApplicationProvider.getApplicationContext<android.app.Application>()
            val imageStore = LocalImageStore(context)
            val repo = FakeWujiuRepository()
            val store = ViewModelStore()
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditorViewModel(
                        repository = repo,
                        imageStore = imageStore,
                        savedStateHandle = SavedStateHandle(mapOf("assetId" to 0L)),
                        ioDispatcher = testDispatcher,
                    ) as T
            }
            val vm = ViewModelProvider(store, factory)[EditorViewModel::class.java]
            advanceUntilIdle()

            vm.setCategory(2L) // "自定义"
            advanceUntilIdle()
            assertEquals(2L, vm.uiState.value.categoryId)

            // Delete custom category 2 -> should auto-fallback to "其他" (id=3)
            repo.deleteCategoryIfUnused(2L)
            advanceUntilIdle()
            assertEquals(3L, vm.uiState.value.categoryId)

            // Integer price capping prevents unbounded input -> Long overflow
            vm.setPrice("12345678901234567890")
            assertEquals("123456789012", vm.uiState.value.priceInput)
            vm.setPrice("999.999")
            assertEquals("999.99", vm.uiState.value.priceInput)

            // Unsaved image inside asset_images/ must be cleaned up when ViewModel clears
            val imageDir = File(context.filesDir, "asset_images").apply { mkdirs() }
            val tempImg = File(imageDir, "unsaved_test.jpg").apply { writeText("img") }
            val bmp = android.graphics.Bitmap.createBitmap(10, 10, android.graphics.Bitmap.Config.ARGB_8888)
            val srcPng = File(context.cacheDir, "editor_import.png")
            srcPng.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
            tempImg.delete()

            vm.importImage(android.net.Uri.fromFile(srcPng))
            advanceUntilIdle()
            val importedPath = vm.uiState.value.imagePath
            assertNotNull(importedPath)
            assertTrue(File(importedPath!!).exists())

            store.clear()
            advanceUntilIdle()
            assertFalse(File(importedPath).exists())
        }
}

private class FakeWujiuRepository : WujiuRepository {
    val categories = MutableStateFlow(
        listOf(
            Category(1L, "电脑", "computer", true),
            Category(2L, "自定义", "other", false),
            Category(3L, "其他", "other", true),
        ),
    )
    private val assets = MutableStateFlow<List<AssetWithCategory>>(emptyList())
    var failOnDelete: Throwable? = null

    fun seedAssets(vararg items: AssetWithCategory) {
        assets.value = items.toList()
    }

    override fun observeCategories(): Flow<List<Category>> = categories
    override fun observeAssets(): Flow<List<AssetWithCategory>> = assets
    override fun observeAsset(id: Long): Flow<AssetWithCategory?> =
        assets.map { list -> list.firstOrNull { it.asset.id == id } }

    override suspend fun getAsset(id: Long): Asset? =
        assets.value.firstOrNull { it.asset.id == id }?.asset

    override suspend fun deleteCategoryIfUnused(id: Long): Boolean {
        categories.value = categories.value.filterNot { it.id == id }
        return true
    }

    override suspend fun hasAssetsInCategory(categoryId: Long): Boolean =
        assets.value.any { it.category.id == categoryId }

    override suspend fun addAsset(asset: NewAsset): Long = 1L
    override suspend fun updateAsset(id: Long, asset: NewAsset) {}
    override suspend fun setInactive(id: Long, inactiveEpochDay: Long?) {}

    override suspend fun deleteAsset(id: Long) {
        failOnDelete?.let { throw it }
        assets.value = assets.value.filterNot { it.asset.id == id }
    }

    override suspend fun addCategory(name: String): Long = 4L

    override suspend fun exportBackup(): WujiuBackupData =
        WujiuBackupData(1, 1000L, emptyList(), emptyList())

    override suspend fun importBackup(
        backup: WujiuBackupData,
        replaceExisting: Boolean,
    ): BackupImportResult = BackupImportResult(
        importedAssets = backup.assets.size,
        skippedDuplicates = 0,
        addedCategories = backup.categories.size,
    )
}
