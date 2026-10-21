package com.frameout.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class FfmpegThumbnailFitTest {
    @Test
    fun scalesLongestSideToMax() {
        assertEquals(256 to 144, FfmpegThumbnailer.fit(1920, 1080, 256))
        assertEquals(144 to 256, FfmpegThumbnailer.fit(1080, 1920, 256))
    }

    @Test
    fun neverUpscalesAndNeverReturnsZero() {
        assertEquals(100 to 50, FfmpegThumbnailer.fit(100, 50, 256))
        assertEquals(256 to 1, FfmpegThumbnailer.fit(100000, 10, 256))
    }
}
