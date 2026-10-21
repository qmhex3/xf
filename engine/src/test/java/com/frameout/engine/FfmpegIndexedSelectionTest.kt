package com.frameout.engine

import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.FfmpegIndexedSelection
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FfmpegIndexedSelectionTest {
    private val index = listOf(
        ListIndexDao.entry(0, 0L, true),
        ListIndexDao.entry(1, 40_000L),
        ListIndexDao.entry(2, 90_000L),
        ListIndexDao.entry(3, 1_100_000L, true),
        ListIndexDao.entry(4, 1_140_000L)
    )

    private fun flow(entries: List<com.frameout.core.db.VideoIndexEntity>, mode: ExtractMode, startUs: Long = 0L, checkpoint: Int = -1) =
        runBlocking { FfmpegIndexedSelection.flow(ListIndexDao.of(entries), 1L, mode, startUs, Long.MAX_VALUE, checkpoint).toList() }

    @Test
    fun everyNMillisUsesPts() = assertEquals(listOf(0, 3), flow(index, ExtractMode.EveryNMillis(1000)))

    @Test
    fun keyframesOnlyUsesIndex() = assertEquals(listOf(0, 3), flow(index, ExtractMode.KeyframesOnly))

    @Test
    fun checkpointSkipsPriorIndexEntries() = assertEquals(listOf(3, 4), flow(index, ExtractMode.EveryFrame, checkpoint = 2))

    @Test
    fun everyNFramesUsesRangeRelativePhase() = assertEquals(listOf(1, 3), flow(index, ExtractMode.EveryNFrames(2), startUs = 40_000L))

    @Test
    fun fixedCountResumeKeepsOriginalDistribution() = assertEquals(listOf(2, 4), flow(index, ExtractMode.FixedCount(3), checkpoint = 1))

    @Test
    fun everyNMillisResumeKeepsTheOriginalGrid() =
        assertEquals(listOf(121, 151, 181, 211, 241, 271), flow(ListIndexDao.steady(300), ExtractMode.EveryNMillis(1000), checkpoint = 100))

    @Test
    fun everyNMillisGapDoesNotCatchUpWithBurst() {
        val gap = (0 until 30).map { ListIndexDao.entry(it, it * 33_333L) } + (0 until 100).map { ListIndexDao.entry(30 + it, 4_000_000L + it * 33_333L) }
        assertEquals(listOf(0, 30, 61, 91, 121), flow(gap, ExtractMode.EveryNMillis(1000)))
    }

    @Test
    fun everyNFramesCountsFromRangeStart() =
        assertEquals(listOf(7, 12, 17, 22, 27), flow(ListIndexDao.steady(30), ExtractMode.EveryNFrames(5), startUs = 7 * 33_333L))

    @Test
    fun fixedCountHitsLastFrame() = assertEquals(listOf(0, 14, 28, 42, 56, 70, 84, 99), flow(ListIndexDao.steady(100), ExtractMode.FixedCount(8)))

    @Test
    fun slidesUseTheSameCandidateStepAsTheSystemDecoder() =
        assertEquals(listOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 80, 88), flow(ListIndexDao.steady(90, keyEvery = 1000), ExtractMode.Slides))

    @Test
    fun sceneResumeKeepsSameCandidatesAsUninterruptedRun() {
        val frames = (0 until 40).map { ListIndexDao.entry(it, it * 20_000L, it == 0) }
        val full = flow(frames, ExtractMode.SceneChange)
        val checkpoint = full[2]
        assertEquals(full.filter { it > checkpoint }, flow(frames, ExtractMode.SceneChange, checkpoint = checkpoint))
    }

    @Test
    fun selectedCountMatchesFlowSize() = runBlocking {
        val dao = ListIndexDao.of(ListIndexDao.steady(300))
        for (mode in listOf(ExtractMode.EveryFrame, ExtractMode.KeyframesOnly, ExtractMode.EveryNFrames(7), ExtractMode.EveryNMillis(500), ExtractMode.FixedCount(9), ExtractMode.SceneChange)) {
            val size = FfmpegIndexedSelection.flow(dao, 1L, mode, 0L, Long.MAX_VALUE, -1).toList().size
            assertEquals(mode.toString(), size, FfmpegIndexedSelection.selectedCount(dao, 1L, mode, 0L, Long.MAX_VALUE, -1))
        }
    }
}
