package com.frameout.engine

import com.frameout.core.db.VideoIndexDao
import com.frameout.core.db.VideoIndexEntity
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractRequest
import com.frameout.engine.extract.IndexSampleSource
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractMemoryBoundTest {
    private class GeneratedIndex(val total: Int) {
        var largestPage = 0
        var queries = 0

        private fun entity(i: Int) = VideoIndexEntity(0L, 1L, i, i * STEP_US, i % KEY_EVERY == 0, (i / KEY_EVERY) * KEY_EVERY, (i / KEY_EVERY) * KEY_EVERY * STEP_US)

        private fun first(startUs: Long): Int = ((startUs + STEP_US - 1L) / STEP_US).toInt()
        private fun last(endUs: Long): Int = minOf(total - 1L, (endUs - 1L) / STEP_US).toInt()

        private fun slice(from: Int, last: Int, limit: Int): List<VideoIndexEntity> {
            queries++
            if (from > last || limit <= 0) return emptyList()
            val to = minOf(last.toLong(), from.toLong() + limit - 1L).toInt()
            return (from..to).map(::entity).also { largestPage = maxOf(largestPage, it.size) }
        }

        fun dao(): VideoIndexDao = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(VideoIndexDao::class.java)) { _, method, args ->
            val a = args ?: emptyArray()
            when (method.name) {
                "rangeOffset" -> slice(first(a[1] as Long) + (a[3] as Int), last(a[2] as Long), 1).firstOrNull()
                "pageRange" -> slice(first(a[1] as Long) + (a[4] as Int), last(a[2] as Long), a[3] as Int)
                "pageRangeAfter" -> slice(maxOf(first(a[1] as Long), (a[3] as Int) + 1), last(a[2] as Long), a[4] as Int)
                "countRange" -> maxOf(0, last(a[2] as Long) - first(a[1] as Long) + 1)
                "firstAtOrAfter" -> slice(maxOf(first(a[1] as Long), first(a[3] as Long)), last(a[2] as Long), 1).firstOrNull()
                else -> error("Unexpected query ${method.name}")
            }
        } as VideoIndexDao
    }

    private fun usedMb(): Long {
        repeat(3) { System.gc() }
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)
    }

    private fun request(mode: ExtractMode) = ExtractRequest(1L, "file:///long.mp4", 0L, 0L, mode, false)

    private class Run(val count: Int, val growthMb: Long, val largestPage: Int)

    private fun drive(total: Int, mode: ExtractMode, checkpointIndex: Int = -1): Run = runBlocking {
        val index = GeneratedIndex(total)
        val source = IndexSampleSource(index.dao(), 1L, total * STEP_US)
        val baseline = usedMb()
        var peak = baseline
        var count = 0
        var previous = -1
        source.sampleFlow(request(mode), -1L, checkpointIndex).collect {
            assertTrue(it.index > previous)
            previous = it.index
            count++
            if (count % SAMPLE_EVERY == 0) peak = maxOf(peak, usedMb())
        }
        Run(count, peak - baseline, index.largestPage)
    }

    @Test
    fun everyFrameOfFourHoursAtSixtyFpsKeepsMemoryFlat() {
        val run = drive(TOTAL, ExtractMode.EveryFrame)
        assertEquals(TOTAL, run.count)
        assertTrue("growth ${run.growthMb}MB", run.growthMb < MAX_GROWTH_MB)
        assertTrue(run.largestPage <= 500)
    }

    @Test
    fun everyNFramesStreamsWithoutBufferingTheIndex() {
        val run = drive(TOTAL, ExtractMode.EveryNFrames(30))
        assertEquals((TOTAL + 29) / 30, run.count)
        assertTrue("growth ${run.growthMb}MB", run.growthMb < MAX_GROWTH_MB)
    }

    @Test
    fun keyframesOnlyStreamsWithoutBufferingTheIndex() {
        val run = drive(TOTAL, ExtractMode.KeyframesOnly)
        assertEquals((TOTAL + KEY_EVERY - 1) / KEY_EVERY, run.count)
        assertTrue("growth ${run.growthMb}MB", run.growthMb < MAX_GROWTH_MB)
        assertTrue(run.largestPage <= 500)
    }

    @Test
    fun timedModeNeverLoadsMoreThanOneSamplePerStep() {
        val run = drive(TOTAL, ExtractMode.EveryNMillis(2_000L))
        assertTrue(run.count > 8_000)
        assertEquals(1, run.largestPage)
        assertTrue("growth ${run.growthMb}MB", run.growthMb < MAX_GROWTH_MB)
    }

    @Test
    fun resumeAfterKillContinuesFromCheckpointWithFlatMemory() {
        val checkpoint = TOTAL / 2 - 1
        val run = drive(TOTAL, ExtractMode.EveryFrame, checkpoint)
        assertEquals(TOTAL / 2, run.count)
        assertTrue("growth ${run.growthMb}MB", run.growthMb < MAX_GROWTH_MB)
    }

    private companion object {
        const val STEP_US = 16_667L
        const val KEY_EVERY = 60
        const val TOTAL = 1_000_000
        const val SAMPLE_EVERY = 100_000
        const val MAX_GROWTH_MB = 48L
    }
}
