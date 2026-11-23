package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

object ImageHelper {
    private const val MAX_DIMENSION_PX = 840
    private const val MAX_JPEG_BYTES = 185 * 1024

    fun createTempCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "bukti_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun compressUriToBase64(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }

            val rawMax = max(boundsOpts.outWidth, boundsOpts.outHeight)
            var sampleSize = 1
            while (rawMax / sampleSize > MAX_DIMENSION_PX * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext Result.failure(IllegalArgumentException("Gagal membaca gambar dari perangkat."))

            val scaled = scaleDownIfNeeded(decoded, MAX_DIMENSION_PX)
            if (scaled !== decoded) decoded.recycle()

            var quality = 78
            val out = ByteArrayOutputStream()
            do {
                out.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                quality -= 10
            } while (out.size() > MAX_JPEG_BYTES && quality >= 30)

            scaled.recycle()
            val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            if (base64.length > 340_000) {
                return@withContext Result.failure(
                    IllegalArgumentException("Ukuran foto melebihi batas maksimal 250 KB setelah kompresi.")
                )
            }
            Result.success(base64)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longest = max(width, height)
        if (longest <= maxDim) return bitmap
        val scale = maxDim.toFloat() / longest.toFloat()
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }
}
