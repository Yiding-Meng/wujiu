package com.mengyiding.wujiu.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class LocalImageStore(private val context: Context) {
    private val imageDirectory: File
        get() = File(context.filesDir, "asset_images").apply { mkdirs() }

    fun import(uri: Uri): String {
        val orientation = readExifOrientation(uri)
        val bitmap = decodeSampledBitmap(uri, orientation = orientation, maxDimension = 1600)
            ?: error("无法读取所选图片")
        val target = File(
            imageDirectory,
            "asset_${System.currentTimeMillis()}_${System.nanoTime()}.jpg",
        )
        try {
            FileOutputStream(target).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)) {
                    "图片保存失败"
                }
            }
            return target.absolutePath
        } catch (t: Throwable) {
            target.delete()
            throw t
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path).canonicalFile
            val baseDir = imageDirectory.canonicalFile
            if (file.path.startsWith(baseDir.path + File.separator)) {
                file.delete()
            }
        }
    }

    private fun readExifOrientation(uri: Uri): Int =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    private fun decodeSampledBitmap(
        uri: Uri,
        orientation: Int,
        maxDimension: Int,
    ): Bitmap? {
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
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val matrix = transformMatrix(
            width = decoded.width,
            height = decoded.height,
            orientation = orientation,
            maxDimension = maxDimension,
        )
        if (matrix.isIdentity) return decoded

        val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (transformed !== decoded) decoded.recycle()
        return transformed
    }

    companion object {
        internal fun swapsDimensions(orientation: Int): Boolean =
            orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                orientation == ExifInterface.ORIENTATION_TRANSVERSE

        internal fun transformMatrix(
            width: Int,
            height: Int,
            orientation: Int,
            maxDimension: Int,
        ): Matrix {
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
            }
            val orientedWidth = if (swapsDimensions(orientation)) height else width
            val orientedHeight = if (swapsDimensions(orientation)) width else height
            val longest = max(orientedWidth, orientedHeight)
            if (longest > maxDimension && longest > 0) {
                val scale = maxDimension.toFloat() / longest
                matrix.postScale(scale, scale)
            }
            return matrix
        }
    }
}
