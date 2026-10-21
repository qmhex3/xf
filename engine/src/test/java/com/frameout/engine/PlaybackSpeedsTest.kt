package com.frameout.engine

import com.frameout.engine.player.PlaybackSpeeds
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedsTest {
    @Test
    fun cyclesThroughAllSpeedsAndWraps() {
        assertEquals(0.5f, PlaybackSpeeds.next(0.25f), 0f)
        assertEquals(1.5f, PlaybackSpeeds.next(1f), 0f)
        assertEquals(0.25f, PlaybackSpeeds.next(2f), 0f)
    }

    @Test
    fun unknownSpeedMovesToNextHigher() {
        assertEquals(1f, PlaybackSpeeds.next(0.75f), 0f)
        assertEquals(0.25f, PlaybackSpeeds.next(10f), 0f)
    }

    @Test
    fun labelsDropTrailingZeros() {
        assertEquals("1\u00D7", PlaybackSpeeds.label(1f))
        assertEquals("0.5\u00D7", PlaybackSpeeds.label(0.5f))
        assertEquals("0.25\u00D7", PlaybackSpeeds.label(0.25f))
        assertEquals("1.5\u00D7", PlaybackSpeeds.label(1.5f))
    }
}
