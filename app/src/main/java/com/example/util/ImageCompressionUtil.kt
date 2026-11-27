package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.annotation.DrawableRes
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressionUtil {

    suspend fun compressUriToBase64(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 480,
        maxBytes: Int = 140_000
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            } ?: error("Unable to open image")

            val rawW = boundsOpts.outWidth.coerceAtLeast(1)
            val rawH = boundsOpts.outHeight.coerceAtLeast(1)
            var sampleSize = 1
            while (max(rawW / sampleSize, rawH / sampleSize) > maxDimensionPx * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: error("Failed to decode image")

            val scale = maxDimensionPx.toFloat() / max(decoded.width, decoded.height).coerceAtLeast(1)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                ).also { if (it !== decoded) decoded.recycle() }
            } else {
                decoded
            }

            var quality = 78
            var bytes: ByteArray
            val out = ByteArrayOutputStream()
            do {
                out.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                bytes = out.toByteArray()
                quality -= 12
            } while (bytes.size > maxBytes && quality >= 24)

            scaled.recycle()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    fun decodeBase64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val raw = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(raw, 0, raw.size)
        } catch (_: Exception) {
            null
        }
    }

    @DrawableRes
    fun drawableForKey(key: String?): Int = when (key) {
        "img_shop_hero" -> R.drawable.img_shop_hero
        "img_rice_grains" -> R.drawable.img_rice_grains
        "img_masoor_dal" -> R.drawable.img_masoor_dal
        "img_mustard_oil" -> R.drawable.img_mustard_oil
        "img_turmeric_spice" -> R.drawable.img_turmeric_spice
        "img_assam_tea" -> R.drawable.img_assam_tea
        "img_fresh_milk" -> R.drawable.img_fresh_milk
        "img_sugar_bowl" -> R.drawable.img_sugar_bowl
        "img_oil_spices" -> R.drawable.img_oil_spices
        "img_tea_dairy" -> R.drawable.img_tea_dairy
        else -> R.drawable.img_rice_grains
    }
}
