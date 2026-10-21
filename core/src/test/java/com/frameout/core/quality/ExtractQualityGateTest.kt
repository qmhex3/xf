package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractQualityGateTest {
    private val size = 64

    private fun sharp(phase: Int) = IntArray(size * size) { i -> if (((i % size) / 2 + (i / size) / 2 + phase) % 2 == 0) 20 else 230 }
    private fun blurred() = IntArray(size * size) { i -> 120 + (i % size) }
    private fun scene(seed: Int) = IntArray(size * size) { i -> ((i % size) * (seed + 3) + (i / size) * seed * 7) % 256 }

    @Test
    fun blurryFrameIsRejectedAfterWarmup() {
        val gate = ExtractQualityGate(true, false)
        repeat(10) { assertTrue(gate.accept(sharp(it % 2), size, size)) }
        assertEquals(false, gate.accept(blurred(), size, size))
        assertTrue(gate.accept(sharp(0), size, size))
    }

    @Test
    fun blurIsAcceptedWhenFilterDisabled() {
        val gate = ExtractQualityGate(false, false)
        repeat(10) { gate.accept(sharp(0), size, size) }
        assertTrue(gate.accept(blurred(), size, size))
    }

    @Test
    fun duplicateFramesAreRejected() {
        val gate = ExtractQualityGate(false, true)
        assertTrue(gate.accept(scene(1), size, size))
        assertEquals(false, gate.accept(scene(1), size, size))
        assertTrue(gate.accept(scene(40), size, size))
    }

    @Test
    fun firstFrameIsAlwaysKept() {
        assertTrue(ExtractQualityGate(true, true).accept(blurred(), size, size))
    }

    @Test
    fun seededHistoryRejectsBlurWithoutWarmup() {
        val gate = ExtractQualityGate(true, false)
        repeat(10) { gate.seed(sharp(it % 2), size, size) }
        assertEquals(false, gate.accept(blurred(), size, size))
    }

    @Test
    fun seededHashRejectsRepeatOfLastSavedFrame() {
        val gate = ExtractQualityGate(false, true)
        gate.seed(scene(5), size, size)
        assertEquals(false, gate.accept(scene(5), size, size))
        assertTrue(gate.accept(scene(11), size, size))
    }

    @Test
    fun plainFramesAreNeverCalledBlurry() {
        val gate = ExtractQualityGate(true, false)
        repeat(10) { assertTrue(gate.accept(sharp(it % 2), size, size)) }
        assertTrue(gate.accept(IntArray(size * size) { 2 }, size, size))
        assertEquals(false, gate.accept(blurred(), size, size))
    }

    @Test
    fun smallChangeInsideARepeatedShotStillCountsAsNew() {
        val gate = ExtractQualityGate(false, true)
        val base = SyntheticFrames.scene(8)
        assertTrue(gate.accept(base, SyntheticFrames.W, SyntheticFrames.H))
        assertEquals(false, gate.accept(SyntheticFrames.noisy(base, 2.0, 4), SyntheticFrames.W, SyntheticFrames.H))
        val moved = SyntheticFrames.withBox(base, SyntheticFrames.W, 100, 40, 14, 14, if (base[40 * SyntheticFrames.W + 100] > 128) 20 else 240)
        assertTrue(gate.accept(moved, SyntheticFrames.W, SyntheticFrames.H))
    }
}
