package com.frameout.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AsciiNameTest {
    @Test
    fun arabicOnlyHasNoUsableName() = assertNull(AsciiName.orNull("فيلم جديد"))

    @Test
    fun latinPartOfMixedNameIsKept() = assertEquals("Movie 2024", AsciiName.orNull("Movie فيلم 2024"))

    @Test
    fun accentsAreFolded() = assertEquals("Cafe", AsciiName.orNull("Café"))

    @Test
    fun folderFallsBackToProjectId() = assertEquals("project_7", AsciiName.folder("مشروع", 7L))

    @Test
    fun blankFolderStaysBlank() = assertEquals("", AsciiName.folder("", 7L))

    @Test
    fun fileNameKeepsExtensionAndFallsBack() {
        assertEquals("frame_0003.jpg", AsciiName.fileName("لقطة.jpg", "frame_0003"))
        assertEquals("shot_01.png", AsciiName.fileName("shot_01.png", "frame_0001"))
    }
}
