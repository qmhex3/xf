package com.frameout.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class SaveLocationTest {
    @Test
    fun joinKeepsBaseWhenSubfolderBlank() {
        assertEquals("Pictures/FrameOut", SaveLocation.join("Pictures/FrameOut/", ""))
    }

    @Test
    fun joinAppendsSanitizedSubfolder() {
        assertEquals("Pictures/FrameOut/clip one", SaveLocation.join("Pictures/FrameOut", "clip/one"))
    }

    @Test
    fun folderNameFallsBackWhenEmpty() {
        assertEquals("video", SaveLocation.folderName(" ..// "))
    }

    @Test
    fun folderNameIsLimited() {
        assertEquals(60, SaveLocation.folderName("a".repeat(200)).length)
    }
}
