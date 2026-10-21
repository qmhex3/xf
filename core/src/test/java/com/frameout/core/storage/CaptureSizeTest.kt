package com.frameout.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureSizeTest {
    @Test
    fun originalKeepsDimensions() {
        assertEquals(3840 to 2160, CaptureSize.target(3840, 2160, CaptureSize.ORIGINAL))
    }

    @Test
    fun halfHalvesBothSides() {
        assertEquals(1920 to 1080, CaptureSize.target(3840, 2160, CaptureSize.HALF))
        assertEquals(1 to 1, CaptureSize.target(1, 1, CaptureSize.HALF))
    }

    @Test
    fun maxLimitsLongestSideKeepingRatio() {
        assertEquals(1920 to 1080, CaptureSize.target(3840, 2160, CaptureSize.MAX_1920))
        assertEquals(1080 to 1920, CaptureSize.target(2160, 3840, CaptureSize.MAX_1920))
        assertEquals(1280 to 720, CaptureSize.target(3840, 2160, CaptureSize.MAX_1280))
    }

    @Test
    fun maxNeverEnlarges() {
        assertEquals(1280 to 720, CaptureSize.target(1280, 720, CaptureSize.MAX_1920))
    }

    @Test
    fun unknownKeyFallsBackToOriginal() {
        assertEquals(640 to 480, CaptureSize.target(640, 480, "bogus"))
        assertEquals(640 to 480, CaptureSize.target(640, 480, null))
    }
}
