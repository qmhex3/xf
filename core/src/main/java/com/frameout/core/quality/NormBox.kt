package com.frameout.core.quality

import java.util.Locale

data class NormBox(val l: Float, val t: Float, val r: Float, val b: Float) {
    val width: Float get() = r - l
    val height: Float get() = b - t

    fun padded(fraction: Float): NormBox {
        val dx = width * fraction
        val dy = height * fraction
        return NormBox((l - dx).coerceAtLeast(0f), (t - dy).coerceAtLeast(0f), (r + dx).coerceAtMost(1f), (b + dy).coerceAtMost(1f))
    }

    companion object {
        const val MIN_SIDE = 0.01f

        fun fromPixels(left: Float, top: Float, right: Float, bottom: Float, imageWidth: Int, imageHeight: Int): NormBox? {
            if (imageWidth <= 0 || imageHeight <= 0) return null
            val box = NormBox(
                (left / imageWidth).coerceIn(0f, 1f), (top / imageHeight).coerceIn(0f, 1f),
                (right / imageWidth).coerceIn(0f, 1f), (bottom / imageHeight).coerceIn(0f, 1f)
            )
            return box.takeIf { it.width >= MIN_SIDE && it.height >= MIN_SIDE }
        }
    }
}

object NormBoxCodec {
    const val MAX_PER_TAG = 8

    fun encode(boxes: List<NormBox>): String =
        boxes.take(MAX_PER_TAG).joinToString(";") { String.format(Locale.ROOT, "%.4f,%.4f,%.4f,%.4f", it.l, it.t, it.r, it.b) }

    fun decode(text: String): List<NormBox> {
        if (text.isBlank()) return emptyList()
        return text.split(';').mapNotNull { part ->
            val v = part.split(',').mapNotNull { it.trim().toFloatOrNull() }
            if (v.size != 4 || v[2] <= v[0] || v[3] <= v[1]) null else NormBox(v[0], v[1], v[2], v[3])
        }
    }
}
