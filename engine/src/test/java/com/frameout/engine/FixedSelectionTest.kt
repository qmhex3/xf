package com.frameout.engine

import com.frameout.engine.extract.FixedSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixedSelectionTest {
    private fun variable(i: Int): Long = if (i < 100) i * 10_000L else 1_000_000L + (i - 99) * 1_000_000L

    @Test
    fun steadyRateMatchesEvenSpacing() {
        val picks = FixedSelection.positions(100, 8) { it * 33_333L }
        assertEquals(listOf(0, 14, 28, 42, 56, 70, 84, 99), picks.toList())
    }

    @Test
    fun variableRateSpreadsByTime() {
        val picks = FixedSelection.positions(110, 5, ::variable)
        assertEquals(0, picks.first())
        assertEquals(109, picks.last())
        assertTrue(picks[1] >= 100)
        assertTrue(picks.toList().zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun countIsAlwaysHonouredWithoutDuplicates() {
        val picks = FixedSelection.positions(10, 10) { if (it < 5) 0L else 1_000L }
        assertEquals((0 until 10).toList(), picks.toList())
    }

    @Test
    fun identicalTimestampsFallBackToIndexSpacing() {
        val picks = FixedSelection.positions(100, 5) { 0L }
        assertEquals(listOf(0, 24, 49, 74, 99), picks.toList())
    }

    @Test
    fun singlePickIsFirstSample() = assertEquals(listOf(0), FixedSelection.positions(50, 1) { it * 1_000L }.toList())
}
