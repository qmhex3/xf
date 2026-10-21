package com.frameout.core.db

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedKindTest {
    @Test
    fun classifiesByName() {
        assertEquals(SavedKind.Gif, savedKindOf("FrameOut_1.GIF"))
        assertEquals(SavedKind.Motion, savedKindOf("MVIMG_2024.jpg"))
        assertEquals(SavedKind.Motion, savedKindOf("shot.MP.jpg"))
        assertEquals(SavedKind.Photo, savedKindOf("FrameOut_1.jpg"))
        assertEquals(SavedKind.Photo, savedKindOf("noextension"))
    }
}
