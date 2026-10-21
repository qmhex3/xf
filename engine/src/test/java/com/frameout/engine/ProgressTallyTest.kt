package com.frameout.engine

import com.frameout.engine.extract.ProgressTally
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTallyTest {
    @Test
    fun totalStartsAtCandidateCount() = assertEquals(10, ProgressTally(10).total(0))

    @Test
    fun totalEqualsDoneWhenAllCandidatesProcessed() {
        val tally = ProgressTally(3)
        repeat(3) { tally.advance() }
        assertEquals(2, tally.total(2))
    }

    @Test
    fun fractionFollowsProcessedCandidatesWhenFewAreSaved() {
        val tally = ProgressTally(1000)
        repeat(500) { tally.advance() }
        val fraction = 10f / tally.total(10)
        assertTrue(fraction in 0.4f..0.6f)
    }

    @Test
    fun fractionFollowsProcessedCandidatesWhenAllAreSaved() {
        val tally = ProgressTally(200)
        repeat(100) { tally.advance() }
        val fraction = 100f / tally.total(100)
        assertTrue(fraction in 0.45f..0.55f)
    }

    @Test
    fun totalNeverBelowDone() {
        val tally = ProgressTally(100, base = 50)
        repeat(10) { tally.advance() }
        assertTrue(tally.total(50) >= 50)
    }
}
