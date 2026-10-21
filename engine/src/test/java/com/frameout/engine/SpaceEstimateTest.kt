package com.frameout.engine

import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.SpaceEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceEstimateTest {
    @Test
    fun plainModesReserveEveryFrame() {
        assertEquals(500, SpaceEstimate.frames(ExtractMode.EveryFrame, 500))
        assertEquals(1, SpaceEstimate.frames(ExtractMode.EveryFrame, 0))
    }

    @Test
    fun sceneAndSlidesDoNotReserveEveryCandidate() {
        assertTrue(SpaceEstimate.frames(ExtractMode.Slides, 14_400) < 1_000)
        assertTrue(SpaceEstimate.frames(ExtractMode.SceneChange, 72_000) < 10_000)
        assertEquals(1, SpaceEstimate.frames(ExtractMode.Slides, 1))
    }
}
