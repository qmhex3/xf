package com.frameout.app

import com.frameout.app.viewer.nextPlayFps
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedTest {
    @Test
    fun cyclesThroughSpeedsAndWraps() {
        assertEquals(5, nextPlayFps(2))
        assertEquals(15, nextPlayFps(10))
        assertEquals(2, nextPlayFps(30))
    }

    @Test
    fun unknownSpeedMovesToNextHigher() {
        assertEquals(10, nextPlayFps(7))
        assertEquals(2, nextPlayFps(100))
    }
}
