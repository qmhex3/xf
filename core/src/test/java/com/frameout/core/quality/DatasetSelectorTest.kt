package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatasetSelectorTest {
    private fun frame(i: Int, ptsMs: Long, score: Double, sig: ByteArray? = null) =
        ScoredFrame(i.toLong(), i, score, sig = sig, ptsUs = ptsMs * 1000L)

    private fun sig(seed: Int) = ByteArray(FrameSignature.SIZE) { ((it * seed) % 251).toByte() }

    @Test
    fun picksSharpestFramePerSecond() {
        val frames = listOf(
            frame(0, 0, 50.0, sig(1)), frame(1, 300, 90.0, sig(2)), frame(2, 700, 60.0, sig(3)),
            frame(3, 1000, 40.0, sig(4)), frame(4, 1500, 80.0, sig(5)), frame(5, 1900, 70.0, sig(6))
        )
        val picks = DatasetSelector.select(frames, DatasetOptions(blurLevel = BlurLevel.Off, dropDuplicates = false))
        assertEquals(listOf(1L, 4L), picks.map { it.id })
    }

    @Test
    fun blurryWindowsAreDropped() {
        val frames = (0 until 6).map { frame(it, it * 1000L, if (it == 2) 2.0 else 100.0, sig(it + 1)) }
        val picks = DatasetSelector.select(frames, DatasetOptions(blurLevel = BlurLevel.Normal, dropDuplicates = false))
        assertEquals(listOf(0L, 1L, 3L, 4L, 5L), picks.map { it.id })
    }

    @Test
    fun offKeepsBlurryWindows() {
        val frames = (0 until 4).map { frame(it, it * 1000L, if (it == 2) 2.0 else 100.0, sig(it + 1)) }
        val picks = DatasetSelector.select(frames, DatasetOptions(blurLevel = BlurLevel.Off, dropDuplicates = false))
        assertEquals(4, picks.size)
    }

    @Test
    fun identicalNeighboursCollapseToSharper() {
        val same = sig(7)
        val frames = listOf(frame(0, 0, 50.0, same), frame(1, 1000, 80.0, same), frame(2, 2000, 60.0, sig(9)))
        val picks = DatasetSelector.select(frames, DatasetOptions(blurLevel = BlurLevel.Off))
        assertEquals(listOf(1L, 2L), picks.map { it.id })
    }

    @Test
    fun sharpnessIsRelativeAndBounded() {
        val frames = (0 until 10).map { frame(it, it * 1000L, (it + 1) * 10.0, sig(it + 1)) }
        val picks = DatasetSelector.select(frames, DatasetOptions(blurLevel = BlurLevel.Off, dropDuplicates = false))
        assertTrue(picks.all { it.sharpness in 0..100 })
        assertEquals(100, picks.last().sharpness)
    }

    @Test
    fun emptyAndZeroInput() {
        assertTrue(DatasetSelector.select(emptyList(), DatasetOptions()).isEmpty())
        val flat = listOf(frame(0, 0, 0.0), frame(1, 1000, 0.0))
        assertTrue(DatasetSelector.select(flat, DatasetOptions()).isEmpty())
    }

    @Test
    fun halfSecondIntervalDoublesDensity() {
        val frames = (0 until 8).map { frame(it, it * 250L, 100.0, sig(it + 1)) }
        val picks = DatasetSelector.select(frames, DatasetOptions(intervalUs = 500_000L, blurLevel = BlurLevel.Off, dropDuplicates = false))
        assertEquals(4, picks.size)
    }

    @Test
    fun blurryWindowsAreReportedAsRejected() {
        val frames = (0 until 6).map { frame(it, it * 1000L, if (it == 2) 2.0 else 100.0, sig(it + 1)) }
        val result = DatasetSelector.split(frames, DatasetOptions(blurLevel = BlurLevel.Normal, dropDuplicates = false))
        assertEquals(listOf(2L), result.rejected.map { it.id })
        assertEquals(5, result.kept.size)
    }

    @Test
    fun offHasNoRejected() {
        val frames = (0 until 4).map { frame(it, it * 1000L, if (it == 2) 2.0 else 100.0, sig(it + 1)) }
        assertTrue(DatasetSelector.split(frames, DatasetOptions(blurLevel = BlurLevel.Off, dropDuplicates = false)).rejected.isEmpty())
    }
}
