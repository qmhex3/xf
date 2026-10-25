package com.frameout.core.quality

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockGridTest {
    private val w = 320
    private val h = 192

    private fun natural(seed: Int): IntArray {
        val base = SyntheticFrames.boxBlur(SyntheticFrames.scene(seed, w, h), w, h, 1)
        return SyntheticFrames.noisy(base, 2.0, seed)
    }

    private fun blocked(src: IntArray, block: Int, amplitude: Double, dx: Int = 0, dy: Int = 0, seed: Int = 7): IntArray {
        val rnd = Random(seed.toLong())
        val cols = (w + dx) / block + 2
        val rows = (h + dy) / block + 2
        val offsets = IntArray(cols * rows) { Math.round(rnd.nextGaussian() * amplitude).toInt() }
        return IntArray(src.size) { i ->
            val x = i % w + dx
            val y = i / w + dy
            (src[i] + offsets[(y / block) * cols + x / block]).coerceIn(0, 255)
        }
    }

    @Test
    fun cleanFramesShowNoGrid() {
        for (seed in 1..6) {
            val grid = BlockGrid.detect(natural(seed), w, h)
            assertTrue("seed $seed strength ${grid.strength}", grid.strength < BlockAware.GATE_LOW)
        }
    }

    @Test
    fun findsPeriodAndPhaseOfACroppedGrid() {
        val grid = BlockGrid.detect(blocked(natural(2), 8, 5.0, dx = 3, dy = 5), w, h)
        assertEquals(8, grid.x.period)
        assertEquals(8, grid.y.period)
        assertEquals(4, grid.x.phase)
        assertEquals(2, grid.y.phase)
        assertTrue(grid.strength > BlockAware.GATE_HIGH)
    }

    @Test
    fun sixteenPixelMacroblocksAreNotReportedAsEight() {
        val grid = BlockGrid.detect(blocked(natural(3), 16, 6.0), w, h)
        assertEquals(16, grid.x.period)
        assertEquals(16, grid.y.period)
    }

    @Test
    fun strengthGrowsWithTheSizeOfTheSteps() {
        val src = natural(4)
        val weak = BlockGrid.detect(blocked(src, 8, 1.0), w, h).strength
        val strong = BlockGrid.detect(blocked(src, 8, 6.0), w, h).strength
        assertTrue("$weak < $strong", strong > weak * 2)
    }

    @Test
    fun aRealStripePatternIsNotACodecGrid() {
        val stripes = IntArray(w * h) { if ((it % w) % 8 < 4) 210 else 30 }
        val grid = BlockGrid.detect(SyntheticFrames.boxBlur(stripes, w, h, 1), w, h)
        assertTrue("strength ${grid.strength}", grid.strength < BlockAware.GATE_LOW)
    }

    @Test
    fun tinyOrShortInputIsSafe() {
        assertFalse(BlockGrid.detect(IntArray(16), 4, 4).x.found)
        assertFalse(BlockGrid.detect(IntArray(10), w, h).y.found)
        assertFalse(BlockGrid.detect(IntArray(w * h) { 100 }, w, h).x.excess > 0.01)
    }

    @Test
    fun straddlingMarksOnlyPixelsTouchingABoundary() {
        val axis = BlockGrid.Axis(8, 3, 1.0)
        val hit = (0 until 16).filter { axis.straddles(it) }
        assertEquals(listOf(3, 4, 11, 12), hit)
        assertFalse(BlockGrid.Axis.NONE.straddles(5))
    }
}
