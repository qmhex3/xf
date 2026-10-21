package com.frameout.engine

import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.FfmpegFrameRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FfmpegFrameRulesTest {
    @Test
    fun missingPtsFallsBackToThirtyFpsSpacing() {
        assertEquals(66_666L, FfmpegFrameRules.resolvePts(-1L, 2))
        assertEquals(500L, FfmpegFrameRules.resolvePts(500L, 2))
    }

    @Test
    fun rangeBoundsAreInclusive() {
        assertTrue(FfmpegFrameRules.inRange(1_000L, 1_000L, 2_000L))
        assertTrue(FfmpegFrameRules.inRange(2_000L, 1_000L, 2_000L))
        assertFalse(FfmpegFrameRules.inRange(999L, 1_000L, 2_000L))
        assertFalse(FfmpegFrameRules.inRange(2_001L, 1_000L, 2_000L))
    }

    @Test
    fun everyNFramesLeavesPhaseToTheSelection() {
        val mode = ExtractMode.EveryNFrames(5)
        assertTrue(listOf(7, 12, 17).all { FfmpegFrameRules.keep(mode, it, 0L, false, null, null) })
    }

    @Test
    fun keyframesOnlyFollowsKeyframeFlag() {
        assertTrue(FfmpegFrameRules.keep(ExtractMode.KeyframesOnly, 4, 0L, true, null, null))
        assertFalse(FfmpegFrameRules.keep(ExtractMode.KeyframesOnly, 4, 0L, false, null, null))
    }

    @Test
    fun everyFrameAlwaysKeeps() {
        assertTrue(FfmpegFrameRules.keep(ExtractMode.EveryFrame, 7, 0L, false, null, null))
    }
}
