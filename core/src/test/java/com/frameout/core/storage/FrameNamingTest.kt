package com.frameout.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameNamingTest {
    @Test
    fun defaultTemplateKeepsLegacyName() {
        assertEquals("FrameOut_7_42.jpg", FrameNaming.render(FrameNaming.DEFAULT, "clip", 7L, 42, 0L, 1, 10, "jpg"))
    }

    @Test
    fun sequenceIsZeroPadded() {
        assertEquals("clip_0003.png", FrameNaming.render("{project}_{n}", "clip", 1L, 9, 0L, 3, 120, "png"))
    }

    @Test
    fun timeTokenIsFilenameSafe() {
        assertEquals("a_00h01m05s250.jpg", FrameNaming.render("a_{time}", "", 1L, 0, 65_250_000L, 1, 1, "jpg"))
    }

    @Test
    fun forbiddenCharactersAreReplaced() {
        assertEquals("a_b_c.jpg", FrameNaming.render("a/b:c", "", 1L, 0, 0L, 1, 1, "jpg"))
    }

    @Test
    fun blankTemplateFallsBack() {
        assertEquals("FrameOut_3_5.jpg", FrameNaming.render("   ", "", 3L, 5, 0L, 1, 1, "jpg").replace("FrameOut_3_5.jpg", "FrameOut_3_5.jpg"))
    }

    @Test
    fun duplicateNamesGetCounters() {
        val used = HashSet<String>()
        assertEquals("a.jpg", FrameNaming.unique("a.jpg", used))
        assertEquals("a_2.jpg", FrameNaming.unique("a.jpg", used))
        assertEquals("A_3.jpg", FrameNaming.unique("A.jpg", used))
    }
}
