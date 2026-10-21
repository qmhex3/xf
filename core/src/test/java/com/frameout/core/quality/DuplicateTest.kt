package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateTest {
    private fun stripes(invert: Boolean, shift: Int = 0) = IntArray(72 * 64) { i ->
        val on = ((i % 72) / 8) % 2 == 0
        (if (on != invert) 200 else 50) + shift
    }

    @Test
    fun brightnessShiftKeepsHash() {
        val a = FrameHash.dHash(stripes(false), 72, 64)
        val b = FrameHash.dHash(stripes(false, 3), 72, 64)
        assertTrue(FrameHash.distance(a, b) <= DuplicateFinder.MAX_DISTANCE)
    }

    @Test
    fun invertedPatternIsFarApart() {
        val a = FrameHash.dHash(stripes(false), 72, 64)
        val b = FrameHash.dHash(stripes(true), 72, 64)
        assertTrue(FrameHash.distance(a, b) > DuplicateFinder.MAX_DISTANCE)
    }

    @Test
    fun tinyImageHashesToZero() {
        assertEquals(0L, FrameHash.dHash(IntArray(16), 4, 4))
    }

    @Test
    fun keepsSharpestOfEachGroup() {
        val frames = listOf(
            HashedFrame(0, 0, 0L, 1.0), HashedFrame(1, 1, 0L, 9.0), HashedFrame(2, 2, 0L, 2.0),
            HashedFrame(3, 3, -1L, 1.0), HashedFrame(4, 4, -1L, 5.0)
        )
        assertEquals(listOf(0L, 2L, 3L), DuplicateFinder.duplicates(frames))
    }

    @Test
    fun emptyInputGivesNoDuplicates() {
        assertTrue(DuplicateFinder.duplicates(emptyList()).isEmpty())
    }

    @Test
    fun keepsTheBestFaceNotJustTheSharpest() {
        val frames = listOf(HashedFrame(0, 0, 0L, 9.0), HashedFrame(1, 1, 0L, 2.0), HashedFrame(2, 2, 0L, 3.0))
        val faces = mapOf(0L to 0.2f, 1L to 0.9f, 2L to 0.4f)
        assertEquals(listOf(0L, 2L), DuplicateFinder.duplicates(frames, faces = faces))
    }

    @Test
    fun nearEqualFacesFallBackToSharpness() {
        val frames = listOf(HashedFrame(0, 0, 0L, 9.0), HashedFrame(1, 1, 0L, 2.0))
        val faces = mapOf(0L to 0.80f, 1L to 0.82f)
        assertEquals(listOf(1L), DuplicateFinder.duplicates(frames, faces = faces))
    }

    @Test
    fun groupsWithoutFacesStillKeepTheSharpest() {
        val frames = listOf(HashedFrame(0, 0, 0L, 1.0), HashedFrame(1, 1, 0L, 5.0))
        assertEquals(listOf(0L), DuplicateFinder.duplicates(frames, faces = emptyMap()))
    }

    private fun hashed(id: Long, luma: IntArray, score: Double) = HashedFrame(id, id.toInt(), 0L, score, sig = FrameSignature.of(luma, SyntheticFrames.W, SyntheticFrames.H))

    @Test
    fun signatureGroupsNoisyRepeatsAndKeepsTheSharpest() {
        val base = SyntheticFrames.scene(5)
        val frames = listOf(
            hashed(0, base, 1.0),
            hashed(1, SyntheticFrames.noisy(base, 2.0, 1), 9.0),
            hashed(2, SyntheticFrames.noisy(base, 2.0, 2), 3.0)
        )
        assertEquals(listOf(0L, 2L), DuplicateFinder.duplicates(frames))
    }

    @Test
    fun signatureKeepsFramesWhereSomethingSmallChanged() {
        val base = SyntheticFrames.scene(5)
        val changed = SyntheticFrames.withBox(base, SyntheticFrames.W, 100, 40, 14, 14, if (base[40 * SyntheticFrames.W + 100] > 128) 20 else 240)
        val frames = listOf(hashed(0, base, 1.0), hashed(1, changed, 1.0))
        assertTrue(DuplicateFinder.duplicates(frames).isEmpty())
    }

    @Test
    fun slowDriftDoesNotChainIntoOneGroup() {
        val base = SyntheticFrames.scene(6)
        val frames = (0..6).map { hashed(it.toLong(), SyntheticFrames.panned(base, SyntheticFrames.W, SyntheticFrames.H, it * 3), 1.0) }
        val dropped = DuplicateFinder.duplicates(frames)
        assertTrue("dropped ${dropped.size} of 7", dropped.size < 6)
    }
}
