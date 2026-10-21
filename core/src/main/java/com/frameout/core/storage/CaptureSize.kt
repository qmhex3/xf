package com.frameout.core.storage

import com.frameout.core.util.SmoothScale
import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.roundToInt

object CaptureSize {
    const val ORIGINAL = "original"
    const val HALF = "half"
    const val MAX_3840 = "max_3840"
    const val MAX_1920 = "max_1920"
    const val MAX_1280 = "max_1280"

    val keys: List<String> = listOf(ORIGINAL, HALF, MAX_3840, MAX_1920, MAX_1280)

    fun normalize(key: String?): String = keys.firstOrNull { it == key } ?: ORIGINAL

    fun target(width: Int, height: Int, key: String?): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return width to height
        return when (val k = normalize(key)) {
            ORIGINAL -> width to height
            HALF -> max(1, width / 2) to max(1, height / 2)
            else -> limited(width, height, k.removePrefix("max_").toInt())
        }
    }

    private fun limited(width: Int, height: Int, limit: Int): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= limit) return width to height
        val ratio = limit.toDouble() / longest
        return max(1, (width * ratio).roundToInt()) to max(1, (height * ratio).roundToInt())
    }

    fun apply(bitmap: Bitmap, key: String?): Bitmap {
        val (w, h) = target(bitmap.width, bitmap.height, key)
        if (w == bitmap.width && h == bitmap.height) return bitmap
        return runCatching { SmoothScale.scaled(bitmap, w, h, true) }.getOrDefault(bitmap)
    }
}
