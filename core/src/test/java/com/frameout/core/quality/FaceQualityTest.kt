package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceQualityTest {
    private val size = 120

    private fun textured(): IntArray = IntArray(size * size) { i ->
        val x = i % size
        val y = i / size
        if ((x + y) % 2 == 0) 90 else 210
    }

    private fun flat(): IntArray = IntArray(size * size) { 150 }

    private fun face(open: Float = 0.95f, smile: Float = 0.1f, yaw: Float = 0f, width: Int = 90) =
        FaceMark(30, 20, 30 + width, 20 + width, open, open, smile, yaw, 0f, 0f)

    @Test
    fun noMarksMeansNoFace() {
        assertEquals(FaceLevel.None, FaceQuality.assess(flat(), size, size, emptyList()).level)
    }

    @Test
    fun openSharpFrontalFaceIsGood() {
        assertEquals(FaceLevel.Good, FaceQuality.assess(textured(), size, size, listOf(face())).level)
    }

    @Test
    fun closedEyesArePoor() {
        assertEquals(FaceLevel.Poor, FaceQuality.assess(textured(), size, size, listOf(face(open = 0.05f))).level)
    }

    @Test
    fun blurryFaceIsPoor() {
        assertEquals(FaceLevel.Poor, FaceQuality.assess(flat(), size, size, listOf(face())).level)
    }

    @Test
    fun turnedAwayFaceIsNotGood() {
        val level = FaceQuality.assess(textured(), size, size, listOf(face(yaw = 40f))).level
        assertTrue(level != FaceLevel.Good)
    }

    @Test
    fun oneClosedEyeLowersOpenness() {
        val mark = FaceMark(30, 20, 90, 80, 0.9f, 0.1f)
        assertEquals(0.1f, mark.eyesOpen, 0.0001f)
    }

    @Test
    fun smileRaisesScoreAndFlagsSmiling() {
        val plain = FaceQuality.assess(textured(), size, size, listOf(face(smile = 0.05f)))
        val smiling = FaceQuality.assess(textured(), size, size, listOf(face(smile = 0.95f)))
        assertTrue(smiling.score > plain.score)
        assertTrue(smiling.smiling)
        assertTrue(!plain.smiling)
    }

    @Test
    fun smallBackgroundFaceIsIgnored() {
        val background = FaceMark(2, 2, 14, 14, 0.1f, 0.1f, 0f)
        val result = FaceQuality.assess(textured(), size, size, listOf(face(), background))
        assertEquals(1, result.faces)
        assertEquals(FaceLevel.Good, result.level)
    }

    @Test
    fun faceScoreChangesPickerChoice() {
        val frames = listOf(
            ScoredFrame(1, 0, 100.0, 0.1f),
            ScoredFrame(2, 1, 80.0, 0.9f),
            ScoredFrame(3, 2, 10.0),
            ScoredFrame(4, 3, 10.0),
            ScoredFrame(5, 4, 10.0)
        )
        assertEquals(listOf(2L), SharpestPicker.pick(frames))
    }

    @Test
    fun candidatesKeepTopPerWindow() {
        val frames = (0 until 10).map { ScoredFrame(it.toLong(), it, it.toDouble()) }
        assertEquals(setOf(4L, 3L, 2L, 9L, 8L, 7L), SharpestPicker.candidates(frames))
    }

    @Test
    fun tinyFaceIsNeverJudgedByEyes() {
        val level = FaceQuality.assess(textured(), size, size, listOf(face(open = 0.05f, width = 45))).level
        assertEquals(FaceLevel.Fair, level)
    }

    @Test
    fun tinyFaceDoesNotClaimASmile() {
        assertTrue(!FaceQuality.assess(textured(), size, size, listOf(face(smile = 0.95f, width = 45))).smiling)
    }

    @Test
    fun distantBlurryFaceIsNotPoor() {
        assertEquals(FaceLevel.Fair, FaceQuality.assess(flat(), size, size, listOf(face(width = 45))).level)
    }

    @Test
    fun squintingSmileIsNotPoor() {
        val level = FaceQuality.assess(textured(), size, size, listOf(face(open = 0.3f, smile = 0.9f))).level
        assertTrue(level != FaceLevel.Poor)
    }

    @Test
    fun narrowedEyesWithoutSmileStayPoor() {
        assertEquals(FaceLevel.Poor, FaceQuality.assess(textured(), size, size, listOf(face(open = 0.3f, smile = 0.1f))).level)
    }

    @Test
    fun turnedHeadEyesAreIgnored() {
        val level = FaceQuality.assess(textured(), size, size, listOf(face(open = 0.05f, yaw = 35f))).level
        assertTrue(level != FaceLevel.Poor)
    }
}
