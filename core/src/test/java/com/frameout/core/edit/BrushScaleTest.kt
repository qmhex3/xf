package com.frameout.core.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrushScaleTest {
    @Test
    fun fractionRoundTrips() {
        listOf(0f, 0.25f, 0.5f, 1f).forEach {
            val size = BrushScale.fromFraction(it, BrushScale.DRAW_MIN, BrushScale.DRAW_MAX)
            assertEquals(it, BrushScale.toFraction(size, BrushScale.DRAW_MIN, BrushScale.DRAW_MAX), 0.001f)
        }
    }

    @Test
    fun endpointsMatchBounds() {
        assertEquals(BrushScale.COVER_MIN, BrushScale.fromFraction(0f, BrushScale.COVER_MIN, BrushScale.COVER_MAX), 1e-6f)
        assertEquals(BrushScale.COVER_MAX, BrushScale.fromFraction(1f, BrushScale.COVER_MIN, BrushScale.COVER_MAX), 1e-5f)
    }

    @Test
    fun pixelsNeverBelowOne() {
        assertEquals(1, BrushScale.pixels(0.0002f, 1000, 800))
        assertEquals(48, BrushScale.pixels(0.06f, 1000, 800))
    }

    @Test
    fun stepMovesExactlyOnePixel() {
        val size = 0.012f
        val up = BrushScale.stepPixels(size, 1, 4000, 3000, BrushScale.DRAW_MIN, BrushScale.DRAW_MAX)
        val down = BrushScale.stepPixels(size, -1, 4000, 3000, BrushScale.DRAW_MIN, BrushScale.DRAW_MAX)
        assertEquals(BrushScale.pixels(size, 4000, 3000) + 1, BrushScale.pixels(up, 4000, 3000))
        assertEquals(BrushScale.pixels(size, 4000, 3000) - 1, BrushScale.pixels(down, 4000, 3000))
    }

    @Test
    fun stepStopsAtBounds() {
        val low = BrushScale.stepPixels(BrushScale.DRAW_MIN, -1, 4000, 3000, BrushScale.DRAW_MIN, BrushScale.DRAW_MAX)
        assertTrue(low >= BrushScale.DRAW_MIN)
        assertEquals(1, BrushScale.pixels(low, 4000, 3000))
    }
}
