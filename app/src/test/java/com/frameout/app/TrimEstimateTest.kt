package com.frameout.app

import com.frameout.app.trim.estimateFrames
import com.frameout.engine.extract.ExtractMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrimEstimateTest {
    private val hour = 3_600_000L

    @Test
    fun sceneAndSlidesAreNotCountedPerCandidate() {
        assertTrue(estimateFrames(hour, ExtractMode.SceneChange) < 2_000)
        assertTrue(estimateFrames(hour, ExtractMode.Slides) < 500)
    }

    @Test
    fun timedModeRoundsUpLikeTheEngine() {
        assertEquals(11, estimateFrames(10_500L, ExtractMode.EveryNMillis(1000L)))
        assertEquals(10, estimateFrames(10_000L, ExtractMode.EveryNMillis(1000L)))
    }

    @Test
    fun fixedCountNeverExceedsAvailableFrames() {
        assertEquals(30, estimateFrames(1_000L, ExtractMode.FixedCount(500)))
        assertEquals(10, estimateFrames(60_000L, ExtractMode.FixedCount(10)))
    }
}
