package com.frameout.engine

import com.frameout.engine.decode.FrameOrientation
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameOrientationTest {
    @Test
    fun rotationIsNormalised() {
        assertEquals(0, FrameOrientation.normalize(0))
        assertEquals(90, FrameOrientation.normalize(90))
        assertEquals(270, FrameOrientation.normalize(-90))
        assertEquals(180, FrameOrientation.normalize(540))
    }

    @Test
    fun portraitVideosSwapDimensions() {
        assertEquals(1080 to 1920, FrameOrientation.outputSize(1920, 1080, 90, 1f))
        assertEquals(1920 to 1080, FrameOrientation.outputSize(1920, 1080, 180, 1f))
    }

    @Test
    fun anamorphicAspectStretchesWidth() {
        assertEquals(1920 to 1080, FrameOrientation.outputSize(1440, 1080, 0, 4f / 3f))
        assertEquals(1f, FrameOrientation.aspectOf(1, 1), 0f)
        assertEquals(1f, FrameOrientation.aspectOf(0, 5), 0f)
        assertEquals(1f, FrameOrientation.aspectOf(100, 101), 0f)
    }
}
