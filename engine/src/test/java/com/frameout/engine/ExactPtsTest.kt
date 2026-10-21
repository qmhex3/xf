package com.frameout.engine

import com.frameout.engine.player.ExactPts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExactPtsTest {
    private val frames120 = listOf(0L, 8_333L, 16_666L, 25_000L, 33_333L)

    @Test
    fun limitCoversTheWholeMillisecondOfThePosition() {
        assertEquals(16_999L, ExactPts.limitUs(16L))
        assertEquals(999L, ExactPts.limitUs(-5L))
    }

    @Test
    fun pickedFrameIsTheLastOneNotAfterThePosition() {
        assertEquals(16_666L, ExactPts.pickDisplayed(frames120, ExactPts.limitUs(16L)))
        assertEquals(8_333L, ExactPts.pickDisplayed(frames120, ExactPts.limitUs(15L)))
        assertEquals(25_000L, ExactPts.pickDisplayed(frames120, ExactPts.limitUs(26L)))
    }

    @Test
    fun steppedPositionsMapBackToTheSameFrame() {
        frames120.forEach { pts -> assertEquals(pts, ExactPts.pickDisplayed(frames120, ExactPts.limitUs(pts / 1000L))) }
    }

    @Test
    fun reorderedSamplesStillYieldTheLatestDisplayedFrame() {
        val decodeOrder = listOf(0L, 33_333L, 16_666L, 8_333L, 25_000L)
        assertEquals(16_666L, ExactPts.pickDisplayed(decodeOrder, ExactPts.limitUs(20L)))
    }

    @Test
    fun positionBeforeTheFirstFrameYieldsNothing() {
        assertNull(ExactPts.pickDisplayed(listOf(40_000L, 50_000L), ExactPts.limitUs(10L)))
    }
}
