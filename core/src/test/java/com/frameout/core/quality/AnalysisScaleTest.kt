package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisScaleTest {
    @Test
    fun sampleKeepsTheLongSideAtOrAboveTheTarget() {
        listOf(3840 to 2160, 1920 to 1080, 7680 to 4320, 1080 to 1920, 1500 to 1500).forEach { (w, h) ->
            val sample = AnalysisScale.sampleFor(w, h, 1024)
            assertTrue(maxOf(w, h) / sample >= 1024)
            assertTrue(maxOf(w, h) / (sample * 2) < 1024)
        }
    }

    @Test
    fun sampleIsOneForSmallImages() {
        assertEquals(1, AnalysisScale.sampleFor(512, 288, 1024))
        assertEquals(1, AnalysisScale.sampleFor(1920, 1080, 1024))
    }

    @Test
    fun sampleIsAPowerOfTwo() {
        val sample = AnalysisScale.sampleFor(7680, 4320, 1024)
        assertEquals(0, sample and (sample - 1))
        assertEquals(4, sample)
    }

    @Test
    fun targetNeverUpscales() {
        assertEquals(512 to 288, AnalysisScale.target(512, 288, 1024))
        assertEquals(1024 to 576, AnalysisScale.target(1024, 576, 1024))
    }

    @Test
    fun targetScalesTheLongSideAndKeepsTheRatio() {
        assertEquals(1024 to 576, AnalysisScale.target(1920, 1080, 1024))
        assertEquals(576 to 1024, AnalysisScale.target(1080, 1920, 1024))
        assertEquals(768 to 432, AnalysisScale.target(1920, 1080, 768))
    }

    @Test
    fun targetStaysAtLeastOnePixel() {
        assertEquals(1 to 1024, AnalysisScale.target(1, 5000, 1024))
    }
}
