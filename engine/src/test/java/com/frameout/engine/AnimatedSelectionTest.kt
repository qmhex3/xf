package com.frameout.engine

import android.graphics.Bitmap
import android.net.Uri
import com.frameout.engine.decode.AnimatedFrameSource
import com.frameout.engine.extract.AnimatedSelection
import com.frameout.engine.extract.ExtractMode
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedSelectionTest {
    private class FakeSource(private val total: Int, private val stepUs: Long = 16_667L) : AnimatedFrameSource {
        override fun open(uri: Uri) = Unit
        override fun close() = Unit
        override fun frameCount(): Int = total
        override fun videoWidth(): Int = 1
        override fun videoHeight(): Int = 1
        override fun durationUs(): Long = total * stepUs
        override fun ptsUs(index: Int): Long = index.coerceIn(0, total - 1) * stepUs
        override fun decodeFrame(index: Int): Pair<Long, Bitmap>? = null
    }

    private fun collect(source: AnimatedFrameSource, mode: ExtractMode, checkpointIndex: Int = -1): List<Int> =
        runBlocking { AnimatedSelection.flow(source, mode, 0L, 0L, -1L, checkpointIndex).toList() }

    @Test
    fun everyFrameKeepsAllFramesInOrder() {
        val picked = collect(FakeSource(5_000), ExtractMode.EveryFrame)
        assertEquals(5_000, picked.size)
        assertTrue(picked.zipWithNext().all { (a, b) -> b == a + 1 })
    }

    @Test
    fun everyNFramesStepsBySampleCount() {
        val picked = collect(FakeSource(12_001), ExtractMode.EveryNFrames(60))
        assertEquals(201, picked.size)
        assertTrue(picked.all { it % 60 == 0 })
    }

    @Test
    fun everyNMillisFollowsPresentationTime() {
        val source = FakeSource(7_200)
        val picked = collect(source, ExtractMode.EveryNMillis(1_000L))
        assertEquals(source.ptsUs(7_199) / 1_000_000L + 1L, picked.size.toLong())
    }

    @Test
    fun fixedCountSpansWholeSequence() {
        val picked = collect(FakeSource(10_000), ExtractMode.FixedCount(10))
        assertEquals(10, picked.size)
        assertEquals(0, picked.first())
        assertEquals(9_999, picked.last())
    }

    @Test
    fun resumeSkipsEverythingUpToCheckpoint() {
        val picked = collect(FakeSource(20_000), ExtractMode.EveryFrame, checkpointIndex = 14_999)
        assertEquals(5_000, picked.size)
        assertEquals(15_000, picked.first())
    }

    @Test
    fun countMatchesFlowOnLongSequence() {
        val source = FakeSource(50_000)
        val mode = ExtractMode.EveryNFrames(3)
        assertEquals(collect(source, mode).size, AnimatedSelection.count(source, mode, 0L, 0L, -1L))
    }

    @Test
    fun trimRangeLimitsSelection() {
        val source = FakeSource(6_000)
        val picked = runBlocking { AnimatedSelection.flow(source, ExtractMode.EveryFrame, 10_000L, 20_000L, -1L).toList() }
        assertTrue(picked.first() >= 10_000_000L / 16_667L)
        assertTrue(source.ptsUs(picked.last()) < 20_000_000L)
    }

    @Test
    fun keyframesOnlyPicksOneFramePerSecond() {
        val source = FakeSource(600)
        val picked = collect(source, ExtractMode.KeyframesOnly)
        assertEquals(source.ptsUs(599) / 1_000_000L + 1L, picked.size.toLong())
        assertEquals(0, picked.first())
    }
}
