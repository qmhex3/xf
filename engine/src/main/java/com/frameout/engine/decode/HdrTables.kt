package com.frameout.engine.decode

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow

internal object HdrTables {
    const val REF_WHITE = 203f
    const val HLG_PEAK = 1000f
    const val TONE_KNEE = 0.55f
    const val DESAT_KNEE = 0.75f
    const val SIZE = 4096
    const val DESAT_STRENGTH = 0.6f
    const val DESAT_EXPONENT = 1.5f
    const val DESAT_STEPS = 1024

    val pq = FloatArray(SIZE + 1) { pqEotf(it / SIZE.toDouble()).toFloat() / REF_WHITE }
    val hlg = FloatArray(SIZE + 1) { hlgScene(it / SIZE.toDouble()).toFloat() }
    val ootf = buildOotf(HLG_PEAK)
    val srgb = FloatArray(SIZE + 1) { (srgbEncode(it / SIZE.toDouble()) * 255.0).toFloat() }

    fun buildOotf(peakNits: Float): FloatArray {
        val gamma = 1.2 + 0.42 * log10(peakNits / HLG_PEAK.toDouble())
        return FloatArray(SIZE + 1) { (it / SIZE.toDouble()).pow(gamma - 1.0).toFloat() * peakNits / REF_WHITE }
    }

    fun index(value: Float): Int = (value.coerceIn(0f, 1f) * SIZE + 0.5f).toInt()

    private fun pqEotf(signal: Double): Double {
        val m1 = 2610.0 / 16384.0
        val m2 = 2523.0 / 32.0
        val c1 = 3424.0 / 4096.0
        val c2 = 2413.0 / 128.0
        val c3 = 2392.0 / 128.0
        val p = signal.pow(1.0 / m2)
        return ((p - c1).coerceAtLeast(0.0) / (c2 - c3 * p).coerceAtLeast(0.0001)).pow(1.0 / m1) * 10000.0
    }

    private fun hlgScene(signal: Double): Double {
        val a = 0.17883277
        val b = 1.0 - 4.0 * a
        val c = 0.5 - a * ln(4.0 * a)
        return if (signal <= 0.5) signal * signal / 3.0 else (exp((signal - c) / a) + b) / 12.0
    }

    private fun srgbEncode(linear: Double): Double =
        if (linear <= 0.0031308) linear * 12.92 else 1.055 * linear.pow(1.0 / 2.4) - 0.055
}
