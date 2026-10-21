package com.frameout.core.edit

object ColorOps {
    private const val LUMA_R = 0.213f
    private const val LUMA_G = 0.715f
    private const val LUMA_B = 0.072f

    fun identity(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    fun saturation(s: Float): FloatArray {
        val inv = 1f - s
        val r = LUMA_R * inv
        val g = LUMA_G * inv
        val b = LUMA_B * inv
        return floatArrayOf(
            r + s, g, b, 0f, 0f,
            r, g + s, b, 0f, 0f,
            r, g, b + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun contrast(contrast: Float, brightness: Float = 0f): FloatArray {
        val shift = (1f - contrast) * 127.5f + brightness * 255f
        return floatArrayOf(
            contrast, 0f, 0f, 0f, shift,
            0f, contrast, 0f, 0f, shift,
            0f, 0f, contrast, 0f, shift,
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun channels(r: Float, g: Float, b: Float, offR: Float = 0f, offG: Float = 0f, offB: Float = 0f): FloatArray = floatArrayOf(
        r, 0f, 0f, 0f, offR,
        0f, g, 0f, 0f, offG,
        0f, 0f, b, 0f, offB,
        0f, 0f, 0f, 1f, 0f
    )

    fun then(first: FloatArray, second: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) sum += second[row * 5 + k] * first[k * 5 + col]
                out[row * 5 + col] = sum
            }
            var offset = second[row * 5 + 4]
            for (k in 0 until 4) offset += second[row * 5 + k] * first[k * 5 + 4]
            out[row * 5 + 4] = offset
        }
        return out
    }

    fun chain(vararg steps: FloatArray): FloatArray = steps.fold(identity()) { acc, m -> then(acc, m) }

    fun blend(m: FloatArray, amount: Float): FloatArray {
        val t = amount.coerceIn(0f, 1f)
        val id = identity()
        return FloatArray(20) { id[it] + (m[it] - id[it]) * t }
    }
}
