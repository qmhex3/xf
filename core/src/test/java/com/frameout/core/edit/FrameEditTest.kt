package com.frameout.core.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameEditTest {
    @Test
    fun defaultEditIsIdentity() {
        assertTrue(FrameEdit(1).isIdentity)
        assertFalse(FrameEdit(1).hasAdjust)
    }

    @Test
    fun adjustmentsBreakIdentity() {
        assertFalse(FrameEdit(1, brightness = 0.1f).isIdentity)
        assertFalse(FrameEdit(1, contrast = 1.2f).isIdentity)
        assertFalse(FrameEdit(1, saturation = 0f).isIdentity)
        assertTrue(FrameEdit(1, saturation = 0f).hasAdjust)
    }

    @Test
    fun marksBreakIdentity() {
        val mark = CoverMark(0f, 0f, 0.5f, 0.5f, CoverStyle.Solid)
        assertFalse(FrameEdit(1, marks = listOf(mark)).isIdentity)
    }

    @Test
    fun copyKeepsMarks() {
        val mark = StrokeMark(-1, 0.01f, listOf(MarkPoint(0.1f, 0.1f)))
        val edit = FrameEdit(1, marks = listOf(mark)).rotate90().toggleFlipH()
        assertEquals(listOf<EditMark>(mark), edit.marks)
    }

    @Test
    fun brushCoverMarkBreaksIdentity() {
        val mark = BrushCoverMark(CoverStyle.Blur, 0.5f, 0.04f, listOf(MarkPoint(0.2f, 0.2f), MarkPoint(0.4f, 0.4f)))
        assertFalse(FrameEdit(1, marks = listOf(mark)).isIdentity)
    }

    @Test
    fun editsWithDifferentCoverStrengthAreNotEqual() {
        val soft = BrushCoverMark(CoverStyle.Pixelate, 0.2f, 0.04f, listOf(MarkPoint(0.5f, 0.5f)))
        val hard = soft.copy(strength = 0.9f)
        assertFalse(FrameEdit(1, marks = listOf(soft)) == FrameEdit(1, marks = listOf(hard)))
        assertEquals(FrameEdit(1, marks = listOf(soft)), FrameEdit(1, marks = listOf(soft.copy())))
    }
}
