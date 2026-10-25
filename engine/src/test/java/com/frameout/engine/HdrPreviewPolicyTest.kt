package com.frameout.engine

import com.frameout.engine.player.HdrPreviewPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HdrPreviewPolicyTest {
    private fun wants(
        enabled: Boolean = true,
        sourceHdr: Boolean = true,
        windowHdr: Boolean = false,
        animated: Boolean = false,
        playing: Boolean = false,
        scrubbing: Boolean = false,
        ready: Boolean = true
    ) = HdrPreviewPolicy.wantsStill(enabled, sourceHdr, windowHdr, animated, playing, scrubbing, ready)

    @Test
    fun pausedHdrSourceOnSdrWindowWantsTheToneMappedStill() {
        assertTrue(wants())
    }

    @Test
    fun sdrSourceNeverWantsIt() {
        assertFalse(wants(sourceHdr = false))
    }

    @Test
    fun hdrWindowNeverWantsIt() {
        assertFalse(wants(windowHdr = true))
    }

    @Test
    fun playbackScrubbingAndNotReadyDropIt() {
        assertFalse(wants(playing = true))
        assertFalse(wants(scrubbing = true))
        assertFalse(wants(ready = false))
    }

    @Test
    fun animatedPreviewAndTheSwitchDropIt() {
        assertFalse(wants(animated = true))
        assertFalse(wants(enabled = false))
    }

    @Test
    fun smallFramesAreKeptAndLargeOnesShrinkKeepingAspect() {
        assertEquals(1280 to 720, HdrPreviewPolicy.scaledSize(1280, 720))
        assertEquals(1920 to 1080, HdrPreviewPolicy.scaledSize(3840, 2160))
        assertEquals(1080 to 1920, HdrPreviewPolicy.scaledSize(2160, 3840))
        assertEquals(0 to 0, HdrPreviewPolicy.scaledSize(0, 0))
    }
}
