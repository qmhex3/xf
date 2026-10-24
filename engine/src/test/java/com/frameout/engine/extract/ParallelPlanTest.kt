package com.frameout.engine.extract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParallelPlanTest {
    @Test
    fun sparseStaticModesAreEligible() {
        assertTrue(ParallelPlan.eligible(ExtractMode.KeyframesOnly, false))
        assertTrue(ParallelPlan.eligible(ExtractMode.FixedCount(50), false))
        assertTrue(ParallelPlan.eligible(ExtractMode.EveryNMillis(1000), false))
        assertTrue(ParallelPlan.eligible(ExtractMode.EveryNFrames(60), false))
    }

    @Test
    fun denseOrStatefulModesStaySerial() {
        assertFalse(ParallelPlan.eligible(ExtractMode.EveryFrame, false))
        assertFalse(ParallelPlan.eligible(ExtractMode.EveryNFrames(2), false))
        assertFalse(ParallelPlan.eligible(ExtractMode.EveryNMillis(100), false))
        assertFalse(ParallelPlan.eligible(ExtractMode.SceneChange, false))
        assertFalse(ParallelPlan.eligible(ExtractMode.Slides, false))
        assertFalse(ParallelPlan.eligible(ExtractMode.KeyframesOnly, true))
    }

    @Test
    fun decoderCountFollowsFrameSizeAndDeviceLimit() {
        assertEquals(1, ParallelPlan.decoders(33_000_000L, 3))
        assertEquals(2, ParallelPlan.decoders(3_700_000L, 3))
        assertEquals(3, ParallelPlan.decoders(2_000_000L, 3))
        assertEquals(1, ParallelPlan.decoders(2_000_000L, 1))
    }
}
