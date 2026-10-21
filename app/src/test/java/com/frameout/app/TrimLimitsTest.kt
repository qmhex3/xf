package com.frameout.app

import com.frameout.app.trim.TrimLimits
import com.frameout.app.trim.TrimOutput
import com.frameout.engine.timeline.TrimRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrimLimitsTest {
    private val live = TrimOutput.Live.maxMs

    @Test
    fun unlimitedOutputsKeepTheRange() {
        val next = TrimRange(0, 60_000, false)
        assertEquals(next, TrimLimits.enforce(TrimRange(0, 10_000, false), next, 0L))
    }

    @Test
    fun draggingEndPastLimitSlidesTheWindow() {
        val result = TrimLimits.enforce(TrimRange(1_000, 4_000, false), TrimRange(1_000, 9_000, false), live)
        assertEquals(TrimRange(9_000 - live, 9_000, false), result)
    }

    @Test
    fun draggingStartPastLimitSlidesTheWindow() {
        val result = TrimLimits.enforce(TrimRange(5_000, 8_000, false), TrimRange(500, 8_000, false), live)
        assertEquals(TrimRange(500, 500 + live, false), result)
    }

    @Test
    fun shrinkingInsideLimitIsAllowed() {
        val next = TrimRange(1_500, 3_000, false)
        assertEquals(next, TrimLimits.enforce(TrimRange(0, 3_000, false), next, live))
    }

    @Test
    fun fitCapsLongSelectionFromItsStart() {
        val result = TrimLimits.fit(TrimRange(10_000, 50_000, false), TrimOutput.Gif.maxMs)
        assertEquals(TrimRange(10_000, 10_000 + TrimOutput.Gif.maxMs, false), result)
    }

    @Test
    fun fitAtKeepsTheWindowAtThePlayhead() {
        val result = TrimLimits.fitAt(TrimRange(0, 60_000, false), live, 40_000)
        assertEquals(TrimRange(40_000, 40_000 + live, false), result)
    }

    @Test
    fun fitAtNearTheEndStaysInsideTheRange() {
        val result = TrimLimits.fitAt(TrimRange(0, 60_000, true), live, 59_500)
        assertEquals(TrimRange(60_000 - live, 60_000, false), result)
    }

    @Test
    fun fitAtOutsideTheRangeFallsBackToItsStart() {
        val result = TrimLimits.fitAt(TrimRange(10_000, 50_000, false), live, 5_000)
        assertEquals(TrimRange(10_000, 10_000 + live, false), result)
    }

    @Test
    fun fitAtLeavesShortSelectionsAlone() {
        val range = TrimRange(10_000, 12_000, false)
        assertEquals(range, TrimLimits.fitAt(range, live, 40_000))
    }

    @Test
    fun initialSelectionNeverStartsAtTheVeryEnd() {
        val result = TrimLimits.initialSelection(59_900, 60_000)
        assertTrue(result.spanMs() >= 1_000)
        assertEquals(60_000, result.endMs)
    }

    @Test
    fun movingStartPastEndCarriesALimitedSelection() {
        val result = TrimLimits.moveStart(TrimRange(0, 3_000, false), 20_000, 60_000, live)
        assertEquals(TrimRange(20_000, 20_000 + live, false), result)
    }

    @Test
    fun limitedOutputsDeclareTheirCaps() {
        assertEquals(3, TrimOutput.Live.seconds)
        assertTrue(TrimOutput.Frames.maxMs == 0L && !TrimOutput.Video.limited)
    }
}
