package com.frameout.engine.decode

internal object CalibrationRule {
    const val ACCEPT = 4.5f

    const val MIN_GAIN = 0.75f

    const val MIN_DELTA = 3f

    const val MAX_ERROR = 60f

    fun pick(autoError: Float, candidateErrors: Map<Int, Float>): Int? {
        val best = candidateErrors.entries.minByOrNull { it.value }
        val lowest = if (best == null) autoError else minOf(autoError, best.value)
        if (lowest.isNaN() || lowest > MAX_ERROR) return null
        if (best == null) return ColorVariants.AUTO
        return if (best.value < autoError * MIN_GAIN && autoError - best.value > MIN_DELTA) best.key else ColorVariants.AUTO
    }

    fun meanAbsDifference(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || a.size != b.size) return Float.NaN
        var sum = 0.0
        for (i in a.indices) sum += kotlin.math.abs(a[i] - b[i])
        return (sum / a.size).toFloat()
    }
}
