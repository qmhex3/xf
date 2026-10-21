package com.frameout.engine

import com.frameout.core.db.VideoIndexEntity
import com.frameout.engine.index.SampleScan
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SampleScanTest {
    private suspend fun collect(scan: SampleScan): List<VideoIndexEntity> {
        val out = ArrayList<VideoIndexEntity>()
        scan.forEachDisplay { out += it }
        return out
    }

    @Test
    fun bFrameStreamIsOrderedByPts() = runTest {
        val scan = SampleScan()
        listOf(0L to true, 120_000L to false, 40_000L to false, 80_000L to false, 240_000L to true, 160_000L to false, 200_000L to false)
            .forEach { scan.add(it.first, it.second) }
        val result = collect(scan)
        assertEquals(listOf(0L, 40_000L, 80_000L, 120_000L, 160_000L, 200_000L, 240_000L), result.map { it.ptsUs })
        assertEquals((0..6).toList(), result.map { it.sampleIndex })
    }

    @Test
    fun nearestKeyframeFollowsDisplayOrder() = runTest {
        val scan = SampleScan()
        listOf(0L to true, 120_000L to false, 40_000L to false, 80_000L to false, 240_000L to true, 160_000L to false, 200_000L to false)
            .forEach { scan.add(it.first, it.second) }
        val result = collect(scan)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 6), result.map { it.nearestKeyframeIndex })
        assertEquals(listOf(0L, 0L, 0L, 0L, 0L, 0L, 240_000L), result.map { it.nearestKeyframePtsUs })
    }

    @Test
    fun ascendingStreamKeepsOrder() = runTest {
        val scan = SampleScan()
        (0 until 10).forEach { scan.add(it * 33_333L, it % 5 == 0) }
        val result = collect(scan)
        assertEquals((0 until 10).map { it * 33_333L }, result.map { it.ptsUs })
        assertEquals(listOf(0, 0, 0, 0, 0, 5, 5, 5, 5, 5), result.map { it.nearestKeyframeIndex })
    }

    @Test
    fun duplicatePtsKeepDecodeOrderAndUniqueIndex() = runTest {
        val scan = SampleScan()
        listOf(100L, 50L, 50L, 150L).forEach { scan.add(it, false) }
        val result = collect(scan)
        assertEquals(listOf(50L, 50L, 100L, 150L), result.map { it.ptsUs })
        assertEquals(result.map { it.sampleIndex }.distinct().size, result.size)
    }

    @Test
    fun growsPastInitialCapacity() = runTest {
        val scan = SampleScan()
        (0 until 10_000).forEach { scan.add((9_999 - it) * 1000L, it == 9_999) }
        val result = collect(scan)
        assertEquals(10_000, scan.size)
        assertEquals(0L, result.first().ptsUs)
        assertEquals(0, result.first().nearestKeyframeIndex)
    }

    @Test
    fun pyramidGopIsSortedByPts() = runTest {
        val scan = SampleScan()
        val decodeOrder = (0 until 3000).flatMap { g -> val b = g * 8 * 40_000L; listOf(b + 8 * 40_000L, b + 4 * 40_000L, b + 2 * 40_000L, b + 40_000L, b + 3 * 40_000L, b + 6 * 40_000L, b + 5 * 40_000L, b + 7 * 40_000L) }
        decodeOrder.forEachIndexed { i, t -> scan.add(t, i % 8 == 0) }
        val result = collect(scan)
        assertEquals(decodeOrder.sorted(), result.map { it.ptsUs })
        assertEquals(result.indices.toList(), result.map { it.sampleIndex })
    }
}
