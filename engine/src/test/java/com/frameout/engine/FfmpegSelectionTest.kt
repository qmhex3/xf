package com.frameout.engine

import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.FfmpegIndexedSelection
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FfmpegSelectionTest {
    private fun flow(frames: Int, mode: ExtractMode, checkpoint: Int = -1) =
        runBlocking { FfmpegIndexedSelection.flow(ListIndexDao.of(ListIndexDao.steady(frames)), 1L, mode, 0L, Long.MAX_VALUE, checkpoint).toList() }

    private fun count(frames: Int, mode: ExtractMode) =
        runBlocking { FfmpegIndexedSelection.selectedCount(ListIndexDao.of(ListIndexDao.steady(frames)), 1L, mode, 0L, Long.MAX_VALUE, -1) }

    @Test
    fun fixedCountKeepsRequestedSchedule() = assertEquals(listOf(0, 5, 11), flow(12, ExtractMode.FixedCount(3)))

    @Test
    fun everyNFramesUsesOriginalPhase() = assertEquals(listOf(0, 3, 6, 9, 12), flow(15, ExtractMode.EveryNFrames(3)))

    @Test
    fun everyNMillisScansAllFramesForVfrPtsFiltering() = assertEquals(listOf(0, 1, 2, 3, 4), flow(5, ExtractMode.EveryNMillis(10)))

    @Test
    fun checkpointSkipsAlreadyProcessedFrames() = assertEquals(listOf(5, 6, 7, 8, 9), flow(10, ExtractMode.EveryFrame, checkpoint = 4))

    @Test
    fun timedCountWithoutEndUsesTimelineLength() = assertEquals(10, count(300, ExtractMode.EveryNMillis(1000)))

    @Test
    fun timedCountNeverExceedsRemainingFrames() = assertEquals(5, count(5, ExtractMode.EveryNMillis(10)))
}
