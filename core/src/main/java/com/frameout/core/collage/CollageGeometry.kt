package com.frameout.core.collage

import kotlin.math.roundToInt
import kotlin.math.sqrt

val COLLAGE_RESOLUTIONS: List<Int> = listOf(1080, 1536, 2048)

const val COLLAGE_DEFAULT_WIDTH = 1536
const val COLLAGE_MIN_WIDTH = 320
const val COLLAGE_MIN_PIXELS = 4_000_000L
const val COLLAGE_MAX_PIXELS = 24_000_000L

enum class CollageAspect(val key: String, val ratio: Float?) {
    Template("template", null),
    Square("1:1", 1f),
    Portrait("4:5", 4f / 5f),
    Story("9:16", 9f / 16f),
    Wide("16:9", 16f / 9f);

    companion object {
        fun fromKey(key: String?): CollageAspect = entries.firstOrNull { it.key == key } ?: Template
    }
}

fun collageUnit(width: Float): Float = width / COLLAGE_REFERENCE_WIDTH

fun sanitizeCollageGap(value: Int): Int = value.coerceIn(0, COLLAGE_GAP_MAX)

fun collageMemoryPixels(maxMemoryBytes: Long): Long = (maxMemoryBytes / 8L).coerceIn(COLLAGE_MIN_PIXELS, COLLAGE_MAX_PIXELS)

fun collageOutputSize(requestedWidth: Int, aspect: Float, maxPixels: Long): Pair<Int, Int> {
    val ratio = 1.0 / aspect
    val maxWidth = sqrt(maxPixels / ratio).toInt().coerceAtLeast(COLLAGE_MIN_WIDTH)
    val width = requestedWidth.coerceAtLeast(COLLAGE_MIN_WIDTH).coerceAtMost(maxWidth)
    return width to (width * ratio).roundToInt().coerceAtLeast(1)
}
