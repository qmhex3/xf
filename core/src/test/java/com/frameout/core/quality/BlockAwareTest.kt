package com.frameout.core.quality

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockAwareTest {
    private val w = 320
    private val h = 192

    private fun natural(seed: Int): IntArray {
        val scene = SyntheticFrames.scene(seed, w, h)
        val soft = IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            (120 + 40 * Math.sin(x / 37.0 + seed) + 30 * Math.cos(y / 29.0) + (scene[i] - 128) * 0.12).toInt().coerceIn(0, 255)
        }
        return SyntheticFrames.noisy(SyntheticFrames.boxBlur(soft, w, h, 1), 1.2, seed)
    }

    private fun blocked(src: IntArray, amplitude: Double, seed: Int = 11): IntArray {
        val rnd = Random(seed.toLong())
        val cols = w / 8 + 2
        val offsets = IntArray(cols * (h / 8 + 2)) { Math.round(rnd.nextGaussian() * amplitude).toInt() }
        return IntArray(src.size) { i -> (src[i] + offsets[((i / w) + 5) / 8 * cols + ((i % w) + 3) / 8]).coerceIn(0, 255) }
    }

    @Test
    fun cleanFrameIsLeftAlone() {
        for (seed in 1..5) assertEquals(1.0, BlockAware.assess(natural(seed), w, h).factor, 0.02)
    }

    @Test
    fun blockStepsInflateTheRawScoreAndTheFactorTakesItBack() {
        val clean = natural(3)
        val damaged = blocked(clean, 4.0)
        val cleanScore = FocusScore.measure(clean, w, h)
        val rawScore = FocusScore.measure(damaged, w, h)
        val factor = BlockAware.assess(damaged, w, h).factor
        assertTrue("raw $rawScore vs clean $cleanScore", rawScore > cleanScore * 2)
        assertTrue("factor $factor", factor < 0.5)
        assertTrue("corrected ${rawScore * factor} vs clean $cleanScore", rawScore * factor < cleanScore * 1.6)
    }

    @Test
    fun blockierFramesNeverOutrankTheirCleanSource() {
        val clean = natural(5)
        val cleanScore = FocusScore.measure(clean, w, h)
        for (amp in listOf(2.0, 4.0, 8.0)) {
            val damaged = blocked(clean, amp)
            val fixed = FocusScore.measure(damaged, w, h) * BlockAware.assess(damaged, w, h).factor
            assertTrue("amp $amp: $fixed vs $cleanScore", fixed < cleanScore * 1.6)
        }
    }

    @Test
    fun genuineBlurStillLosesToSharpAfterCorrection() {
        val sharp = natural(6)
        val soft = SyntheticFrames.boxBlur(sharp, w, h, 2)
        val a = FocusScore.measure(sharp, w, h) * BlockAware.assess(sharp, w, h).factor
        val b = FocusScore.measure(soft, w, h) * BlockAware.assess(soft, w, h).factor
        assertTrue(a > b)
    }

    @Test
    fun maskedScoreOnAGridlessFrameMatchesTheOrdinaryScore() {
        val src = natural(7)
        val plain = FocusScore.detail(src, w, h).score
        val masked = FocusScore.detail(src, w, h, BlockGrid.Grid(BlockGrid.Axis(8, 2, 0.5), BlockGrid.Axis(8, 2, 0.5))).score
        assertTrue("plain $plain masked $masked", masked > plain * 0.6 && masked < plain * 1.4)
    }

    @Test
    fun smallImagesAreNeutral() {
        assertEquals(1.0, BlockAware.assess(IntArray(64 * 64) { it % 7 }, 64, 64).factor, 0.0)
    }
}
