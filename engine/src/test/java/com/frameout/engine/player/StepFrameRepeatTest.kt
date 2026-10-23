package com.frameout.engine.player

import org.junit.Assert.assertEquals
import org.junit.Test

class StepFrameRepeatTest {
    private val frames = (0..30).map { it * 33_333L }

    private fun forward(ms: Long): Long = frames.filter { SystemStepper.isAfterCurrent(it, ms) }.min() / 1000L
    private fun backward(ms: Long): Long = frames.filter { SystemStepper.isBeforeCurrent(it, ms) }.max() / 1000L

    @Test
    fun repeatedForwardStepsAlwaysAdvanceOneFrame() {
        var ms = 0L
        val seen = ArrayList<Long>()
        repeat(10) { ms = forward(ms); seen.add(ms) }
        assertEquals((1..10).map { it * 33_333L / 1000L }, seen)
    }

    @Test
    fun repeatedBackwardStepsAlwaysGoBackOneFrame() {
        var ms = 10 * 33_333L / 1000L
        val seen = ArrayList<Long>()
        repeat(5) { ms = backward(ms); seen.add(ms) }
        assertEquals((9 downTo 5).map { it * 33_333L / 1000L }, seen)
    }

    @Test
    fun forwardThenBackReturnsToTheSameFrame() {
        val start = 5 * 33_333L / 1000L
        assertEquals(start, backward(forward(start)))
    }
}
