package com.frameout.core.export

import android.graphics.Bitmap
import android.os.Build
import java.io.ByteArrayOutputStream

object WebpFrameEncoder {
    fun encode(bitmap: Bitmap, quality: Int): ByteArray {
        val out = ByteArrayOutputStream()
        check(bitmap.compress(format(Build.VERSION.SDK_INT), quality.coerceIn(1, 100), out)) { "WebP encode failed" }
        return out.toByteArray()
    }

    @Suppress("DEPRECATION")
    private fun format(sdk: Int): Bitmap.CompressFormat = if (sdk >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
}
