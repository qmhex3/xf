package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusRegionTest {
    private val car = NormBox(0.50f, 0.40f, 0.70f, 0.60f)

    @Test
    fun squareRegionKeepsTheDestinationAspectAndContainsTheBox() {
        val r = FocusRegion.pixels(car, 1000, 500, 1f)
        assertEquals(r.w, r.h)
        assertTrue(r.x <= 500 && r.x + r.w >= 700)
        assertTrue(r.y <= 200 && r.y + r.h >= 300)
    }

    @Test
    fun regionStaysInsideTheImage() {
        val corner = NormBox(0.90f, 0.90f, 1f, 1f)
        val r = FocusRegion.pixels(corner, 640, 360, 1f)
        assertTrue(r.x >= 0 && r.y >= 0 && r.x + r.w <= 640 && r.y + r.h <= 360)
    }

    @Test
    fun tinyBoxesDoNotZoomPastFourTimes() {
        val r = FocusRegion.pixels(NormBox(0.50f, 0.50f, 0.52f, 0.52f), 1000, 1000, null)
        assertTrue(r.w >= 250 && r.h >= 250)
    }

    @Test
    fun fitRegionFollowsTheBoxNotTheCell() {
        val r = FocusRegion.pixels(NormBox(0.1f, 0.1f, 0.9f, 0.5f), 1000, 1000, null)
        assertTrue(r.w > r.h)
    }

    @Test
    fun wholeFrameBoxReturnsNearlyTheWholeFrame() {
        val r = FocusRegion.pixels(NormBox(0f, 0f, 1f, 1f), 800, 600, 800f / 600f)
        assertEquals(800, r.w)
        assertEquals(600, r.h)
    }

    @Test
    fun largestPicksTheBiggestBoxAndUnionCoversAll() {
        val small = NormBox(0f, 0f, 0.1f, 0.1f)
        assertEquals(car, FocusRegion.largest(listOf(small, car)))
        assertNull(FocusRegion.largest(emptyList()))
        val u = FocusRegion.union(listOf(small, car))
        assertNotNull(u)
        assertEquals(0f, u!!.l, 0.0001f)
        assertEquals(0.70f, u.r, 0.0001f)
        assertNull(FocusRegion.union(emptyList()))
    }
}
