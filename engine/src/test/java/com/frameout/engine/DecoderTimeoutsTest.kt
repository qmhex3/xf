package com.frameout.engine

import com.frameout.engine.decode.DecoderTimeouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecoderTimeoutsTest {
    @Test
    fun smallFramesKeepBaseTimeout() {
        assertEquals(15_000L, DecoderTimeouts.outputMs(640, 360))
    }

    @Test
    fun largeFramesGetLongerTimeout() {
        assertTrue(DecoderTimeouts.outputMs(7680, 4320) > DecoderTimeouts.outputMs(1920, 1080))
    }

    @Test
    fun timeoutIsCapped() {
        assertEquals(90_000L, DecoderTimeouts.outputMs(30000, 30000))
        assertEquals(15_000L, DecoderTimeouts.outputMs(0, -5))
    }
}
