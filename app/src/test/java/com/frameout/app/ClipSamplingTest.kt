package com.frameout.app

import com.frameout.app.trim.ClipSampling
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipSamplingTest {
    @Test
    fun shortClipUsesTwelveFramesPerSecond() {
        val plan = ClipSampling.plan(0L, 1000L)
        assertNotNull(plan)
        assertEquals(83, plan!!.frameMs)
        assertEquals(13, plan.timesMs.size)
        assertEquals(0L, plan.timesMs.first())
        assertEquals(996L, plan.timesMs.last())
        assertFalse(plan.capped)
    }

    @Test
    fun samplesStartAtRangeStartAndStayInside() {
        val plan = ClipSampling.plan(5000L, 7000L)!!
        assertEquals(5000L, plan.timesMs.first())
        assertTrue(plan.timesMs.all { it in 5000L until 7000L })
    }

    @Test
    fun midLengthClipSlowsDownToStayUnderFrameLimit() {
        val plan = ClipSampling.plan(0L, 20_000L)!!
        assertEquals(167, plan.frameMs)
        assertEquals(ClipSampling.MAX_FRAMES, plan.timesMs.size)
        assertFalse(plan.capped)
    }

    @Test
    fun longClipIsCappedToTwentySeconds() {
        val plan = ClipSampling.plan(1000L, 61_000L)!!
        assertTrue(plan.capped)
        assertTrue(plan.timesMs.size <= ClipSampling.MAX_FRAMES)
        assertTrue(plan.timesMs.last() < 1000L + ClipSampling.MAX_SPAN_MS)
    }

    @Test
    fun tooShortOrEmptyRangesGiveNoPlan() {
        assertNull(ClipSampling.plan(2000L, 2000L))
        assertNull(ClipSampling.plan(500L, 100L))
        assertNull(ClipSampling.plan(5000L, 5050L))
    }
}
