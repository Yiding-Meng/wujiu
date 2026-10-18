package com.mengyiding.wujiu.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class LocalImageStore(private val context: Context) {
    private val imageDirectory: File
        get() = File(context.filesDir, "asset_images").apply { mkdirs() }

    fun import(uri: Uri): String {
        val bitmap = decodeSampledBitmap(uri, maxDimension = 1600)
            ?: error("无法读取所选图片")
        val target = File(
            imageDirectory,
            "asset_${System.currentTimeMillis()}_${System.nanoTime()}.jpg",
        )
        FileOutputStream(target).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)) {
                "图片保存失败"
            }
        }
        bitmap.recycle()
        return target.absolutePath
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    private fun decodeSampledBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > maxDimension * 2 ||
            bounds.outHeight / sampleSize > maxDimension * 2
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longest
        val resized = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        if (resized !== bitmap) bitmap.recycle()
        return resized
    }
}
