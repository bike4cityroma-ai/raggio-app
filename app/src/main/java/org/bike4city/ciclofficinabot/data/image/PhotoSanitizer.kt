package org.bike4city.ciclofficinabot.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import org.bike4city.ciclofficinabot.domain.model.ChatPhoto

object PhotoSanitizer {
    private const val MAX_DIMENSION = 1_280
    private const val MAX_BYTES = 1_250_000

    fun sanitize(context: Context, uri: Uri): ChatPhoto {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Immagine non leggibile" }
            BitmapFactory.decodeStream(input, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Formato immagine non supportato" }
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_DIMENSION * 2) sample *= 2
        val decoded = resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Immagine non leggibile" }
            requireNotNull(BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sample }))
        }
        val rotation = resolver.openInputStream(uri).use { input ->
            if (input == null) 0 else runCatching { ExifInterface(input).rotationDegrees }.getOrDefault(0)
        }
        val rotated = if (rotation == 0) decoded else Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation.toFloat()) }, true
        ).also { if (it !== decoded) decoded.recycle() }
        val scale = minOf(1f, MAX_DIMENSION.toFloat() / max(rotated.width, rotated.height))
        val width = (rotated.width * scale).roundToInt().coerceAtLeast(1)
        val height = (rotated.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = if (width == rotated.width && height == rotated.height) rotated
        else Bitmap.createScaledBitmap(rotated, width, height, true).also { rotated.recycle() }
        var quality = 86
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().use { output ->
                check(scaled.compress(Bitmap.CompressFormat.JPEG, quality, output))
                output.toByteArray()
            }
            quality -= 8
        } while (bytes.size > MAX_BYTES && quality >= 46)
        scaled.recycle()
        require(bytes.size <= MAX_BYTES) { "Immagine troppo grande anche dopo la compressione" }
        return ChatPhoto(bytes, width, height)
    }
}
