package com.frameout.core.quality

import kotlin.math.pow

object BlockAware {
    const val GATE_LOW = 0.04
    const val GATE_HIGH = 0.10
    const val MIN_FACTOR = 0.01

    class Result(val factor: Double, val grid: BlockGrid.Grid) {
        val blocky: Boolean get() = factor < 1.0
    }

    val NEUTRAL = Result(1.0, BlockGrid.Grid.NONE)

    fun assess(luma: IntArray, width: Int, height: Int): Result {
        val grid = BlockGrid.detect(luma, width, height)
        val weight = ((grid.strength - GATE_LOW) / (GATE_HIGH - GATE_LOW)).coerceIn(0.0, 1.0)
        if (weight <= 0.0) return Result(1.0, grid)
        val plain = FocusScore.detail(luma, width, height).score
        if (plain <= 0.0) return Result(1.0, grid)
        val aware = FocusScore.detail(luma, width, height, grid).score
        return Result((aware / plain).coerceIn(MIN_FACTOR, 1.0).pow(weight), grid)
    }
}
