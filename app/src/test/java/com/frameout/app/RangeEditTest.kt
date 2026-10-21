package com.frameout.app

import com.frameout.app.trim.RangeEdit
import com.frameout.app.trim.TrimOutput
import com.frameout.app.trim.TypedTime
import com.frameout.engine.timeline.TrimRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RangeEditTest {
    @Test
    fun lengthKeepsStartWhenItFits() {
        val result = RangeEdit.withLength(TrimRange(2_000, 4_000, false), 60_000, 0L, 5_000)
        assertEquals(TrimRange(2_000, 7_000, false), result)
    }

    @Test
    fun lengthShiftsStartBackNearTheEnd() {
        val result = RangeEdit.withLength(TrimRange(58_000, 60_000, false), 60_000, 0L, 10_000)
        assertEquals(TrimRange(50_000, 60_000, false), result)
    }

    @Test
    fun lengthIsCappedByOutputLimit() {
        val result = RangeEdit.withLength(TrimRange(0, 2_000, false), 60_000, TrimOutput.Live.maxMs, 20_000)
        assertEquals(TrimRange(0, TrimOutput.Live.maxMs, false), result)
    }

    @Test
    fun lengthIsCappedByVideoDuration() {
        val result = RangeEdit.withLength(TrimRange(1_000, 2_000, false), 4_000, 0L, 30_000)
        assertEquals(TrimRange(0, 4_000, false), result)
    }

    @Test
    fun presetsRespectLimitAndAddTheMaximum() {
        assertEquals(listOf(1_000L, 2_000L, 3_000L), RangeEdit.presets(60_000, TrimOutput.Live.maxMs))
        assertEquals(listOf(1_000L, 2_000L, 3_000L, 5_000L, 8_000L), RangeEdit.presets(60_000, TrimOutput.Gif.maxMs))
    }

    @Test
    fun presetsNeverExceedShortVideos() {
        assertEquals(listOf(1_000L, 2_000L), RangeEdit.presets(2_500, 0L))
    }

    @Test
    fun presetMatchNeedsASelection() {
        assertTrue(RangeEdit.matches(TrimRange(0, 5_010, false), 5_000))
        assertFalse(RangeEdit.matches(TrimRange(0, 5_000, true), 5_000))
    }

    @Test
    fun typedDigitsReadLikeAStopwatch() {
        assertEquals("00:05", TypedTime.label(TypedTime.digitsOf("5")))
        assertEquals("01:30", TypedTime.label(TypedTime.digitsOf("130")))
        assertEquals("1:05:30", TypedTime.label(TypedTime.digitsOf("10530")))
        assertEquals(90_000L, TypedTime.millis("130"))
        assertEquals(3_930_000L, TypedTime.millis("10530"))
        assertEquals(0L, TypedTime.millis(""))
    }

    @Test
    fun secondsOverflowIntoMinutes() {
        assertEquals(90_000L, TypedTime.millis("90"))
    }

    @Test
    fun appendedDigitsFollowTheCurrentText() {
        assertEquals("5", TypedTime.appended("00:12.345", "00:12.3455"))
        assertEquals("", TypedTime.appended("00:12.345", "00:12.34"))
        assertEquals("", TypedTime.appended("00:12.345", "x"))
    }
}
