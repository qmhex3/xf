package com.frameout.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CaptureNamingTest {
    @Test
    fun defaultTemplateUsesProjectAndTime() {
        assertEquals("FrameOut_clip_00h00m05s250.jpg", CaptureNaming.name(FrameNaming.DEFAULT, "clip", 5250L, 1, "jpg"))
    }

    @Test
    fun customTemplateWithoutTimeGetsTimeSuffix() {
        assertEquals("shot_00h01m00s000.png", CaptureNaming.name("shot", "clip", 60000L, 1, "png"))
    }

    @Test
    fun templateWithSequenceIsKept() {
        assertEquals("a_0003.jpg", CaptureNaming.name("a_{n}", "clip", 0L, 3, "jpg"))
    }

    @Test
    fun differentPositionsGiveDifferentNames() {
        assertNotEquals(CaptureNaming.name("", "v", 1000L, 1, "jpg"), CaptureNaming.name("", "v", 1001L, 1, "jpg"))
    }

    @Test
    fun unsafeProjectCharactersAreReplaced() {
        assertEquals("FrameOut_a_b_00h00m00s000.jpg", CaptureNaming.name("", "a/b", 0L, 1, "jpg"))
    }
}
