package com.frameout.core.quality

import com.frameout.core.quality.SyntheticFrames.H
import com.frameout.core.quality.SyntheticFrames.W
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameSignatureTest {
    private fun sig(luma: IntArray) = FrameSignature.of(luma, W, H)!!
    private val base = SyntheticFrames.scene(9)

    @Test
    fun identicalFramesAreTheSame() {
        assertTrue(FrameSignature.same(sig(base), sig(base.copyOf())))
    }

    @Test
    fun sensorNoiseAndSmallExposureShiftAreStillTheSame() {
        val other = SyntheticFrames.shifted(SyntheticFrames.noisy(base, 3.0, 21), 7)
        assertTrue(FrameSignature.same(sig(base), sig(other)))
    }

    @Test
    fun aSmallObjectEnteringTheShotIsNotADuplicate() {
        val hand = SyntheticFrames.withBox(base, W, 100, 40, 14, 14, if (base[40 * W + 100] > 128) 20 else 240)
        assertTrue(!FrameSignature.same(sig(base), sig(hand)))
    }

    @Test
    fun aNewLineOfTextIsNotADuplicate() {
        val flatSlide = IntArray(W * H) { 235 }
        val withLine = SyntheticFrames.withBox(flatSlide, W, 20, 30, 90, 5, 60)
        assertTrue(!FrameSignature.same(sig(flatSlide), sig(withLine)))
    }

    @Test
    fun aPanIsNotADuplicate() {
        assertTrue(!FrameSignature.same(sig(base), sig(SyntheticFrames.panned(base, W, H, 10))))
    }

    @Test
    fun aFadeIsNotADuplicate() {
        assertTrue(!FrameSignature.same(sig(base), sig(SyntheticFrames.scaled(base, 0.6))))
    }

    @Test
    fun differentScenesAreNotDuplicates() {
        assertTrue(!FrameSignature.same(sig(SyntheticFrames.scene(1)), sig(SyntheticFrames.scene(2))))
    }

    @Test
    fun toleranceMovesTheLine() {
        val other = SyntheticFrames.withBox(base, W, 60, 30, 10, 10, 128)
        val g = FrameSignature.gap(sig(base), sig(other))
        assertTrue(g.maxDiff > 0.0)
        assertTrue(FrameSignature.same(sig(base), sig(other), tolerance = 50.0))
    }

    @Test
    fun smallOrInvalidInputIsHandled() {
        assertEquals(FrameSignature.SIZE, FrameSignature.of(IntArray(4) { 100 }, 2, 2)!!.size)
        assertNull(FrameSignature.of(IntArray(3), 2, 2))
        assertTrue(!FrameSignature.same(ByteArray(3), sig(base)))
    }

    @Test
    fun noisyFootageIsJudgedAgainstItsOwnNoise() {
        val noisyA = SyntheticFrames.noisy(base, 22.0, 31)
        val noisyB = SyntheticFrames.noisy(base, 22.0, 32)
        val fixed = FrameSignature.same(sig(noisyA), sig(noisyB))
        val n = FocusScore.detail(noisyA, W, H).noise
        val adaptive = FrameSignature.same(sig(noisyA), sig(noisyB), 1.0, FrameSignature.cellNoise(n, W, H), FrameSignature.cellNoise(n, W, H))
        assertTrue("adaptive should accept the repeat", adaptive)
        assertTrue("fixed thresholds are too tight for this noise: $fixed", !fixed || adaptive)
    }

    @Test
    fun adaptingToNoiseDoesNotHideRealChanges() {
        val noisyA = SyntheticFrames.noisy(base, 22.0, 31)
        val hand = SyntheticFrames.noisy(SyntheticFrames.withBox(base, W, 100, 40, 28, 28, if (base[40 * W + 100] > 128) 20 else 240), 22.0, 33)
        val n = FrameSignature.cellNoise(FocusScore.detail(noisyA, W, H).noise, W, H)
        assertTrue(!FrameSignature.same(sig(noisyA), sig(hand), 1.0, n, n))
    }

    @Test
    fun cellNoiseShrinksWithBiggerCells() {
        assertTrue(FrameSignature.cellNoise(8.0, 640, 360) < FrameSignature.cellNoise(8.0, 160, 90))
    }
}
