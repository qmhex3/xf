package com.frameout.engine

import com.frameout.engine.edit.buildKeptRanges
import com.frameout.engine.edit.contains
import com.frameout.engine.edit.containsPts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReassemblePlanTest {
    @Test
    fun continuousPtsSingleRange() {
        val ranges = buildKeptRanges(listOf(0L, 33_000L, 66_000L), gapUs = 50_000L)
        assertEquals(1, ranges.size)
        assertEquals(0L, ranges[0].startUs)
        assertTrue(ranges.containsPts(33_000L))
        assertFalse(ranges.containsPts(200_000L))
    }

    @Test
    fun gapSplitsRanges() {
        val ranges = buildKeptRanges(
            listOf(0L, 33_000L, 200_000L, 233_000L),
            gapUs = 50_000L
        )
        assertEquals(2, ranges.size)
        assertTrue(ranges[0].contains(0L))
        assertTrue(ranges[1].contains(200_000L))
        assertFalse(ranges.containsPts(100_000L))
    }

    @Test
    fun emptyInput() {
        assertTrue(buildKeptRanges(emptyList()).isEmpty())
    }
}
