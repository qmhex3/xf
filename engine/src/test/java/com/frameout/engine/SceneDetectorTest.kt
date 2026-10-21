package com.frameout.engine

import com.frameout.engine.extract.SceneDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class SceneDetectorTest {
    private fun value(seed: Int, x: Int, y: Int): Float =
        0.5f + 0.25f * sin(x * (0.011f + 0.007f * seed) + seed) +
            0.2f * cos(y * (0.013f + 0.005f * seed) + seed * 2) +
            0.1f * sin((x + y) * (0.004f + 0.003f * seed))

    private fun signature(seedA: Int, seedB: Int = seedA, mix: Float = 0f, gain: Float = 1f): FloatArray =
        SceneDetector.signature(320, 180) { x, y ->
            val v = ((1f - mix) * value(seedA, x, y) + mix * value(seedB, x, y)) * gain
            (v.coerceIn(0f, 1f) * 255f).toInt()
        }

    private fun saved(frames: List<FloatArray>): List<Int> {
        var state: FloatArray? = frames.first()
        val kept = mutableListOf(0)
        for (i in 1 until frames.size) {
            if (SceneDetector.changed(state, frames[i])) {
                kept += i
                state = frames[i]
            } else {
                state = SceneDetector.carry(state, frames[i])
            }
        }
        return kept
    }

    @Test
    fun staticShotKeepsOnlyFirstFrame() = assertEquals(listOf(0), saved(List(40) { signature(1) }))

    @Test
    fun flatFramesAreNotCuts() {
        val flat = SceneDetector.signature(320, 180) { _, _ -> 25 }
        assertEquals(listOf(0), saved(List(30) { flat }))
    }

    @Test
    fun hardCutIsSavedOnceAfterConfirmation() {
        val kept = saved(List(20) { signature(1) } + List(20) { signature(3) })
        assertEquals(2, kept.size)
        assertTrue(kept[1] in 21..24)
    }

    @Test
    fun exposureRampIsNotACut() =
        assertEquals(listOf(0), saved(List(40) { signature(1, gain = 1f + 0.4f * minOf(1f, it / 20f)) }))

    @Test
    fun dissolveIsSavedOnceItSettles() {
        val frames = List(60) { i ->
            val mix = if (i < 15) 0f else if (i >= 35) 1f else (i - 15) / 20f
            signature(1, 3, mix)
        }
        val kept = saved(frames)
        assertEquals(2, kept.size)
        assertTrue(kept[1] >= 35)
    }

    @Test
    fun rejectedCandidateIsRetriedOnNextFrame() {
        val frames = List(20) { signature(1) } + List(20) { signature(3) }
        var state: FloatArray? = frames[0]
        var vetoedAt = -1
        var acceptedAt = -1
        for (i in 1 until frames.size) {
            if (SceneDetector.changed(state, frames[i])) {
                if (vetoedAt < 0) {
                    vetoedAt = i
                    state = SceneDetector.carry(state, frames[i])
                    continue
                }
                acceptedAt = i
                break
            }
            state = SceneDetector.carry(state, frames[i])
        }
        assertEquals(vetoedAt + 1, acceptedAt)
    }
}
