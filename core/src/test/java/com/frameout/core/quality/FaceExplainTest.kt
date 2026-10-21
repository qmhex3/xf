package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Test

class FaceExplainTest {
    private val size = 120
    private fun textured() = IntArray(size * size) { i -> if (((i % size) + (i / size)) % 2 == 0) 90 else 210 }
    private fun flat() = IntArray(size * size) { 150 }
    private fun face(open: Float = 0.95f, yaw: Float = 0f, width: Int = 90) = FaceMark(30, 20, 30 + width, 20 + width, open, open, 0.1f, yaw, 0f, 0f)
    private fun factor(luma: IntArray, mark: FaceMark) = FaceExplain.factor(FaceQuality.assess(luma, size, size, listOf(mark)))

    @Test
    fun goodFaceHasNothingToBlame() = assertEquals(FaceFactor.AllGood, factor(textured(), face()))

    @Test
    fun closedEyesAreNamed() = assertEquals(FaceFactor.EyesClosed, factor(textured(), face(open = 0.05f)))

    @Test
    fun blurIsNamed() = assertEquals(FaceFactor.Blurry, factor(flat(), face()))

    @Test
    fun turnedAwayIsNamed() = assertEquals(FaceFactor.TurnedAway, factor(textured(), face(yaw = 44f)))

    @Test
    fun smallFaceSaysEyesCannotBeVerified() = assertEquals(FaceFactor.EyesUnverified, factor(textured(), face(width = 45)))
}
