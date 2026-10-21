package com.frameout.engine

import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractRequest
import com.frameout.engine.extract.FfmpegIndexedSelection
import com.frameout.engine.extract.IndexSampleSource
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class IndexSampleSourceTest {
    private val entries = ListIndexDao.steady(600)
    private val dao = ListIndexDao.of(entries)
    private val durationUs = 600 * 33_333L
    private val modes = listOf(
        ExtractMode.EveryFrame, ExtractMode.KeyframesOnly, ExtractMode.EveryNFrames(7), ExtractMode.EveryNMillis(500),
        ExtractMode.FixedCount(11), ExtractMode.SceneChange, ExtractMode.Slides
    )

    private fun request(mode: ExtractMode, startMs: Long = 0L, endMs: Long = 0L) = ExtractRequest(1L, "file:///a.mp4", startMs, endMs, mode, false)

    private fun indices(mode: ExtractMode, checkpointIndex: Int = -1, startMs: Long = 0L, endMs: Long = 0L): List<Int> = runBlocking {
        IndexSampleSource(dao, 1L, durationUs).sampleFlow(request(mode, startMs, endMs), -1L, checkpointIndex).toList().map { it.index }
    }

    @Test
    fun everyModeMatchesTheFfmpegSelection() = runBlocking {
        for (mode in modes) {
            val viaFfmpeg = FfmpegIndexedSelection.flow(dao, 1L, mode, 0L, durationUs, -1).toList()
            assertEquals(mode.toString(), viaFfmpeg, indices(mode))
        }
    }

    @Test
    fun resumeEmitsExactlyTheTailOfAnUninterruptedRun() {
        for (mode in modes) {
            val full = indices(mode)
            val checkpoint = full[full.size / 3]
            assertEquals(mode.toString(), full.filter { it > checkpoint }, indices(mode, checkpoint))
        }
    }

    @Test
    fun selectedCountMatchesFlowSize() = runBlocking {
        val source = IndexSampleSource(dao, 1L, durationUs)
        for (mode in modes) {
            assertEquals(mode.toString(), indices(mode).size, source.selectedCount(request(mode), -1L, -1))
        }
    }

    @Test
    fun trimRangeLimitsTheSelection() {
        val picked = indices(ExtractMode.EveryFrame, startMs = 1_000L, endMs = 2_000L)
        assertEquals(31, picked.first())
        assertEquals(true, picked.all { entries[it].ptsUs in 1_000_000L until 2_000_000L })
    }

    @Test
    fun everyNMillisFollowsPresentationTime() {
        val picked = indices(ExtractMode.EveryNMillis(1_000L))
        assertEquals(listOf(0, 31, 61, 91), picked.take(4))
    }
}
