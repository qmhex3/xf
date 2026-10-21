package com.frameout.engine.decode

import android.graphics.Bitmap
import android.graphics.Canvas

internal object AlphaFlatten {
    fun pixels(pixels: IntArray, count: Int = pixels.size) {
        for (i in 0 until count) {
            val p = pixels[i]
            val a = p ushr 24
            if (a == 255) continue
            val inverse = 255 - a
            val r = (((p shr 16) and 255) * a + 255 * inverse + 127) / 255
            val g = (((p shr 8) and 255) * a + 255 * inverse + 127) / 255
            val b = ((p and 255) * a + 255 * inverse + 127) / 255
            pixels[i] = (255 shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    fun bitmap(source: Bitmap): Bitmap? {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        out.eraseColor(WHITE)
        Canvas(out).drawBitmap(source, 0f, 0f, null)
        return out
    }

    private const val WHITE = -1
}
