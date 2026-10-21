package com.frameout.engine.decode

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object GainCodec {
    const val LOG_LOW = -4.2f
    const val LOG_HIGH = 4.0f
    const val EPS = 1f / 64f
    private const val SCALE = 65535f / (LOG_HIGH - LOG_LOW)

    fun codeOf(logGain: Float): Int = ((logGain.coerceIn(LOG_LOW, LOG_HIGH) - LOG_LOW) * SCALE + 0.5f).toInt()

    fun logOf(code: Int): Float = LOG_LOW + code / SCALE

    class Grid(lowCode: Int, highCode: Int) {
        val step: Float
        val zero: Int

        init {
            val logLo = min(logOf(lowCode), 0f)
            val logHi = max(logOf(highCode), 0f)
            val width = max(logHi - logLo, 1e-4f)
            zero = (-logLo / width * 255f).roundToInt().coerceIn(0, 255)
            var s = 0f
            if (zero > 0) s = max(s, -logLo / zero)
            if (zero < 255) s = max(s, logHi / (255 - zero))
            step = max(s, 1e-5f)
        }

        val ratioMin: Float get() = exp(-step * zero)

        val ratioMax: Float get() = exp(step * (255 - zero))

        fun byteOf(code: Int): Int = (logOf(code) / step).roundToInt().plus(zero).coerceIn(0, 255)
    }
}
