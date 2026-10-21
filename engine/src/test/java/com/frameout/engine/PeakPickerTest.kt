package com.frameout.engine

import com.frameout.engine.audio.LevelAccumulator
import com.frameout.engine.audio.MarkerNav
import com.frameout.engine.audio.PeakPicker
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeakPickerTest {
    private fun quiet(size: Int) = FloatArray(size) { 0.03f }

    @Test
    fun silenceHasNoPeaks() = assertTrue(PeakPicker.pick(FloatArray(100), 100, 10, 1000).isEmpty())

    @Test
    fun tooShortInputHasNoPeaks() = assertTrue(PeakPicker.pick(floatArrayOf(0.5f, 0.9f), 100, 10, 1000).isEmpty())

    @Test
    fun loudBurstIsFoundAtItsWindow() {
        val levels = quiet(200).also { it[80] = 0.9f }
        assertEquals(listOf(8000L), PeakPicker.pick(levels, 100, 10, 1000))
    }

    @Test
    fun nearbyPeaksKeepOnlyTheLoudest() {
        val levels = quiet(200).also { it[50] = 0.6f; it[53] = 0.9f }
        assertEquals(listOf(5300L), PeakPicker.pick(levels, 100, 10, 1000))
    }

    @Test
    fun maxPeaksKeepsStrongestAndSortsByTime() {
        val levels = quiet(300).also { it[200] = 0.9f; it[100] = 0.8f; it[20] = 0.5f }
        assertEquals(listOf(10000L, 20000L), PeakPicker.pick(levels, 100, 2, 1000))
    }

    @Test
    fun navigationSkipsMarkersNearThePlayhead() {
        val markers = listOf(1000L, 5000L, 9000L)
        assertEquals(9000L, MarkerNav.next(markers, 5000))
        assertEquals(1000L, MarkerNav.previous(markers, 5000))
        assertEquals(null, MarkerNav.next(markers, 9000))
        assertEquals(null, MarkerNav.previous(markers, 1000))
    }

    @Test
    fun accumulatorProducesOneLevelPerWindow() {
        val acc = LevelAccumulator(100)
        acc.configure(12_000, 1, false)
        val buffer = ByteBuffer.allocate(12_000 * 2).order(ByteOrder.nativeOrder())
        repeat(12_000) { buffer.putShort(if (it < 6_000) 0 else 16_384) }
        acc.feed(buffer, 0, buffer.capacity())
        val levels = acc.finish()
        assertEquals(10, levels.size)
        assertEquals(0f, levels.first(), 0.001f)
        assertEquals(0.5f, levels.last(), 0.01f)
    }
}
