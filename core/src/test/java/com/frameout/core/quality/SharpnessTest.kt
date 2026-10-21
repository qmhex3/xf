package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharpnessTest {
    private fun checker(size: Int, cell: Int) = IntArray(size * size) { i ->
        if (((i % size) / cell + (i / size) / cell) % 2 == 0) 0 else 255
    }

    @Test
    fun flatImageScoresZero() {
        assertEquals(0.0, Sharpness.score(IntArray(64 * 64) { 128 }, 64, 64), 0.0)
    }

    @Test
    fun sharpEdgesScoreHigherThanSmoothGradient() {
        val sharp = Sharpness.score(checker(64, 1), 64, 64)
        val smooth = Sharpness.score(IntArray(64 * 64) { (it % 64) * 4 }, 64, 64)
        assertTrue(sharp > smooth)
    }

    @Test
    fun tinyImageIsSafe() {
        assertEquals(0.0, Sharpness.score(IntArray(4), 2, 2), 0.0)
    }

    @Test
    fun pickerChoosesSharpestPerWindow() {
        val frames = (0 until 20).map { ScoredFrame(it.toLong(), it, if (it == 3 || it == 12) 100.0 else 1.0) }
        assertEquals(listOf(3L, 5L, 12L, 15L), SharpestPicker.pick(frames))
    }

    @Test
    fun pickerHandlesEmptyAndWindowBounds() {
        assertTrue(SharpestPicker.pick(emptyList()).isEmpty())
        assertEquals(5, SharpestPicker.windowFor(10))
        assertEquals(30, SharpestPicker.windowFor(100000))
    }

    @Test
    fun pickerCollapsesNeighbouringWinnersOfAStaticShot() {
        val sig = FrameSignature.of(SyntheticFrames.scene(3), SyntheticFrames.W, SyntheticFrames.H)
        val frames = (0 until 20).map { ScoredFrame(it.toLong(), it, if (it == 3) 50.0 else if (it == 8) 60.0 else 1.0, sig = sig) }
        assertEquals(listOf(8L), SharpestPicker.pick(frames))
    }

    @Test
    fun pickerKeepsWinnersOfDifferentShots() {
        val a = FrameSignature.of(SyntheticFrames.scene(3), SyntheticFrames.W, SyntheticFrames.H)
        val b = FrameSignature.of(SyntheticFrames.scene(4), SyntheticFrames.W, SyntheticFrames.H)
        val frames = (0 until 20).map { ScoredFrame(it.toLong(), it, if (it == 3 || it == 12) 50.0 else 1.0, sig = if (it < 10) a else b) }
        assertEquals(listOf(3L, 12L), SharpestPicker.pick(frames).filter { it == 3L || it == 12L })
    }
}
