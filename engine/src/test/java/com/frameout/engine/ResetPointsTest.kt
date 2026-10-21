package com.frameout.engine

import com.frameout.engine.decode.ResetPoints
import org.junit.Assert.assertEquals
import org.junit.Test

class ResetPointsTest {
    private val points = intArrayOf(0, 5, 9, 20)

    @Test
    fun picksTheLastPointAtOrBeforeTheIndex() {
        assertEquals(0, ResetPoints.lastAtOrBefore(points, 0))
        assertEquals(0, ResetPoints.lastAtOrBefore(points, 4))
        assertEquals(5, ResetPoints.lastAtOrBefore(points, 5))
        assertEquals(9, ResetPoints.lastAtOrBefore(points, 19))
        assertEquals(20, ResetPoints.lastAtOrBefore(points, 400))
    }

    @Test
    fun emptyPointsFallBackToTheStart() {
        assertEquals(0, ResetPoints.lastAtOrBefore(IntArray(0), 7))
    }
}
