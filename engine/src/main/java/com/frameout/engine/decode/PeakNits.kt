package com.frameout.engine.decode

internal object PeakNits {
    const val DEFAULT = 1000f
    const val MIN = 400f
    const val MAX_PQ = 10000f
    const val MAX_HLG = 4000f

    fun resolve(maxContent: Int, maxMastering: Int, hlg: Boolean): Float {
        val nits = if (maxContent > 0) maxContent else maxMastering
        if (nits <= 0) return DEFAULT
        return nits.toFloat().coerceIn(MIN, if (hlg) MAX_HLG else MAX_PQ)
    }
}
