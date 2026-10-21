package com.frameout.engine

import com.frameout.engine.player.SystemStepper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemStepperPickTest {
    private val ms = 1000L

    @Test
    fun pickNextIgnoresDecodeOrder() {
        val after = listOf(80L * ms, 40L * ms, 60L * ms, 20L * ms)
        assertEquals(20L, SystemStepper.pickNext(after, 1))
        assertEquals(60L, SystemStepper.pickNext(after, 3))
    }

    @Test
    fun pickNextClampsToLastAvailable() {
        assertEquals(40L, SystemStepper.pickNext(listOf(20L * ms, 40L * ms), 10))
        assertNull(SystemStepper.pickNext(emptyList(), 1))
    }

    @Test
    fun pickPreviousCountsBackFromTarget() {
        val before = listOf(0L, 20L * ms, 60L * ms, 40L * ms)
        assertEquals(60L, SystemStepper.pickPrevious(before, 1))
        assertEquals(20L, SystemStepper.pickPrevious(before, 3))
    }

    @Test
    fun pickPreviousClampsToFirstAvailable() {
        assertEquals(20L, SystemStepper.pickPrevious(listOf(20L * ms, 40L * ms), 10))
        assertNull(SystemStepper.pickPrevious(emptyList(), 2))
    }
}
