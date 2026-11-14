package com.mengyiding.wujiu.data

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.mengyiding.wujiu.data.image.LocalImageStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalImageStoreTest {
    @Test
    fun `exif orientation transform matrix rotates portrait images and scales oversized dimensions`() {
        assertTrue(LocalImageStore.swapsDimensions(ExifInterface.ORIENTATION_ROTATE_90))
        assertTrue(LocalImageStore.swapsDimensions(ExifInterface.ORIENTATION_ROTATE_270))
        assertTrue(LocalImageStore.swapsDimensions(ExifInterface.ORIENTATION_TRANSPOSE))
        assertTrue(LocalImageStore.swapsDimensions(ExifInterface.ORIENTATION_TRANSVERSE))
        assertFalse(LocalImageStore.swapsDimensions(ExifInterface.ORIENTATION_ROTATE_180))

        val normal = LocalImageStore.transformMatrix(
            width = 400,
            height = 300,
            orientation = ExifInterface.ORIENTATION_NORMAL,
            maxDimension = 1600,
        )
        assertTrue(normal.isIdentity)

        val rotated = LocalImageStore.transformMatrix(
            width = 2400,
            height = 1200,
            orientation = ExifInterface.ORIENTATION_ROTATE_90,
            maxDimension = 1600,
        )
        assertFalse(rotated.isIdentity)
        val rect = android.graphics.RectF(0f, 0f, 2400f, 1200f)
        rotated.mapRect(rect)
        assertEquals(800f, rect.width(), 1f)
        assertEquals(1600f, rect.height(), 1f)
    }

    @Test
    fun `import saves inside imageDirectory and delete refuses outside paths`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val store = LocalImageStore(context)

        val sourceFile = File(context.cacheDir, "sample_source.png")
        val bmp = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        FileOutputStream(sourceFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bmp.recycle()

        val importedPath = store.import(Uri.fromFile(sourceFile))
        val importedFile = File(importedPath)
        assertTrue(importedFile.exists())

        val outsideFile = File(context.filesDir, "must_not_delete.txt").apply { writeText("keep") }
        store.delete(outsideFile.absolutePath)
        assertTrue(outsideFile.exists())

        store.delete(importedPath)
        assertFalse(importedFile.exists())
    }
}
