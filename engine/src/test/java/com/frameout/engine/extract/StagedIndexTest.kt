package com.frameout.engine.extract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StagedIndexTest {
    @Test
    fun completeIndexKeepsExactCount() {
        val view = IndexView(1000, 10_000_000L, true)
        assertEquals(500, scaleCandidates(500, view, 0L, Long.MAX_VALUE, 10_000_000L))
    }

    @Test
    fun partialIndexScalesByCoveredSpan() {
        val view = IndexView(100, 1_000_000L, false)
        assertEquals(1000, scaleCandidates(100, view, 0L, Long.MAX_VALUE, 10_000_000L))
    }

    @Test
    fun partialIndexRespectsEndBound() {
        val view = IndexView(100, 1_000_000L, false)
        assertEquals(500, scaleCandidates(100, view, 0L, 5_000_000L, 10_000_000L))
    }

    @Test
    fun unknownDurationFallsBackToCount() {
        val view = IndexView(100, 1_000_000L, false)
        assertEquals(100, scaleCandidates(100, view, 0L, Long.MAX_VALUE, 0L))
    }

    @Test
    fun estimateNeverBelowCount() {
        val view = IndexView(100, 9_000_000L, false)
        assertTrue(scaleCandidates(100, view, 0L, Long.MAX_VALUE, 5_000_000L) >= 100)
    }

    @Test
    fun retargetRaisesTotalWithoutGoingBelowProcessed() {
        val tally = ProgressTally(100)
        repeat(60) { tally.advance() }
        tally.retarget(10)
        assertEquals(60, tally.total(60))
        tally.retarget(400)
        assertTrue(tally.total(60) > 60)
    }
}
