package com.frameout.core.quality

import android.graphics.Bitmap

object Sharpness {
    fun score(luma: IntArray, width: Int, height: Int): Double {
        if (width < 3 || height < 3 || luma.size < width * height) return 0.0
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val i = row + x
                val lap = (4 * luma[i] - luma[i - 1] - luma[i + 1] - luma[i - width] - luma[i + width]).toDouble()
                sum += lap
                sumSq += lap * lap
                n++
            }
        }
        val mean = sum / n
        return sumSq / n - mean * mean
    }

    fun lumaOf(bitmap: Bitmap): IntArray {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            pixels[i] = (77 * ((p shr 16) and 0xFF) + 150 * ((p shr 8) and 0xFF) + 29 * (p and 0xFF)) shr 8
        }
        return pixels
    }
}
