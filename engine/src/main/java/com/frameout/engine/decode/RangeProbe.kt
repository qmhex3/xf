package com.frameout.engine.decode

internal class RangeProbe {
    private var full = false
    private var extreme = 0L
    private var seen = 0L

    fun reset() {
        full = false
        extreme = 0L
        seen = 0L
    }

    fun resolve(params: ColorParams, wide: Boolean, width: Int, height: Int, luma: (Int, Int) -> Int): ColorParams {
        if (params.rangeDeclared) return params
        if (!full) full = observe(wide, width, height, luma)
        return if (full) params.withFullRange() else params
    }

    private fun observe(wide: Boolean, width: Int, height: Int, luma: (Int, Int) -> Int): Boolean {
        if (width < GRID || height < GRID) return false
        val low = if (wide) 8 else 2
        val high = if (wide) 1015 else 253
        var outside = 0
        for (gy in 0 until GRID) {
            val y = (gy * (height - 1)) / (GRID - 1)
            for (gx in 0 until GRID) {
                val v = luma((gx * (width - 1)) / (GRID - 1), y)
                if (v <= low || v >= high) outside++
            }
        }
        val total = GRID * GRID
        if (outside * 1000 > total * FRAME_PER_MILLE) return true
        extreme += outside
        seen += total
        return seen >= total * MIN_FRAMES && extreme * 1000 > seen * TOTAL_PER_MILLE
    }

    private companion object {
        const val GRID = 48
        const val FRAME_PER_MILLE = 15
        const val TOTAL_PER_MILLE = 4
        const val MIN_FRAMES = 3
    }
}
