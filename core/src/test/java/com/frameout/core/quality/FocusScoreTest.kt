package com.frameout.core.quality

import com.frameout.core.quality.SyntheticFrames.H
import com.frameout.core.quality.SyntheticFrames.W
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusScoreTest {
    private fun score(luma: IntArray, w: Int = W, h: Int = H) = FocusScore.measure(luma, w, h)

    @Test
    fun flatAndTinyFramesScoreZeroAndNeverCrash() {
        assertEquals(0.0, score(IntArray(W * H) { 128 }), 0.0)
        assertEquals(0.0, FocusScore.measure(IntArray(4), 2, 2), 0.0)
        assertEquals(0.0, FocusScore.measure(IntArray(10), W, H), 0.0)
    }

    @Test
    fun sharpBeatsBlurredAtEveryBlurLevel() {
        val base = SyntheticFrames.scene(1)
        var previous = score(base)
        for (radius in 1..4) {
            val next = score(SyntheticFrames.boxBlur(base, W, H, radius))
            assertTrue("radius $radius: $next should be below $previous", next < previous)
            previous = next
        }
    }

    @Test
    fun pureNoiseIsNotMistakenForDetail() {
        val flat = IntArray(W * H) { 128 }
        val noise = score(SyntheticFrames.noisy(flat, 6.0, 7))
        val real = score(SyntheticFrames.scene(1))
        assertTrue("noise $noise vs real $real", noise < real * 0.03)
    }

    @Test
    fun noisySharpFrameBeatsNoisyBlurredFrame() {
        val base = SyntheticFrames.scene(2)
        val sharp = score(SyntheticFrames.noisy(base, 5.0, 11))
        val soft = score(SyntheticFrames.noisy(SyntheticFrames.boxBlur(base, W, H, 3), 5.0, 12))
        assertTrue("sharp $sharp vs soft $soft", sharp > soft * 2.0)
    }

    @Test
    fun heavyNoiseCannotLiftABlurredFrameAboveACleanSharpOne() {
        val base = SyntheticFrames.scene(3)
        val cleanSharp = score(base)
        val noisyBlur = score(SyntheticFrames.noisy(SyntheticFrames.boxBlur(base, W, H, 2), 12.0, 5))
        assertTrue("clean sharp $cleanSharp vs noisy blur $noisyBlur", cleanSharp > noisyBlur)
    }

    @Test
    fun exposureDoesNotDecideTheWinner() {
        val base = SyntheticFrames.scene(4)
        val normal = score(base)
        val dark = score(SyntheticFrames.scaled(base, 0.55))
        val ratio = dark / normal
        assertTrue("ratio $ratio", ratio in 0.7..1.4)
    }

    @Test
    fun sharpSubjectOnSoftBackgroundBeatsUniformMush() {
        val sharpPatch = IntArray(W * H) { 128 }
        for (y in 0 until H / 2) for (x in 0 until W / 2) sharpPatch[y * W + x] = if (((x / 2) + (y / 2)) % 2 == 0) 20 else 235
        val mush = IntArray(W * H) { i -> if ((((i % W) / 2) + ((i / W) / 2)) % 2 == 0) 100 else 150 }
        assertTrue(score(sharpPatch) > score(mush))
    }

    @Test
    fun oneDirectionalDetailIsReportedAsMotionBlur() {
        val bars = FocusScore.detail(SyntheticFrames.verticalBars(W, H, 4), W, H)
        val checks = FocusScore.detail(SyntheticFrames.checker(W, H, 4), W, H)
        assertTrue("bars ${bars.balance}", bars.balance < 0.2)
        assertTrue("checks ${checks.balance}", checks.balance > 0.8)
        assertTrue(checks.score > bars.score)
    }

    @Test
    fun plainFramesAreFlagged() {
        assertTrue(FocusScore.detail(IntArray(W * H) { 3 }, W, H).plain)
        assertTrue(!FocusScore.detail(SyntheticFrames.scene(1), W, H).plain)
    }

    @Test
    fun noiseLevelIsEstimated() {
        val flat = IntArray(W * H) { 128 }
        val sigma = FocusScore.detail(SyntheticFrames.noisy(flat, 5.0, 3), W, H).noise
        assertEquals(5.0, sigma, 1.0)
    }
}
