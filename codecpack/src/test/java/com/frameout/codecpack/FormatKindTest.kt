package com.frameout.codecpack

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatKindTest {
    @Test
    fun ffmpegContainersListed() {
        val needs = setOf("mkv", "avi", "wmv", "flv", "ts")
        assertEquals(5, needs.size)
    }

    @Test
    fun formatInfoDefaults() {
        val info = FormatInfo("image/gif", "gif", FormatKind.Gif)
        assertEquals(FormatKind.Gif, info.kind)
        assertEquals("", info.message)
        assertEquals(MediaFailure.None, info.failure)
    }
}
