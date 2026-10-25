package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyframeTieTest {
    private fun frame(id: Long, score: Double, key: Boolean = false) = ScoredFrame(id, id.toInt(), score, isKeyframe = key)

    @Test
    fun keyframeWinsWhenScoresAreWithinTheMargin() {
        val picked = KeyframeTie.best(listOf(frame(1, 100.0), frame(2, 99.5, key = true), frame(3, 80.0)))
        assertEquals(2L, picked.id)
    }

    @Test
    fun keyframeNeverBeatsAClearlySharperFrame() {
        val picked = KeyframeTie.best(listOf(frame(1, 100.0), frame(2, 97.0, key = true)))
        assertEquals(1L, picked.id)
    }

    @Test
    fun withoutKeyframesTheSharpestWins() {
        assertEquals(3L, KeyframeTie.best(listOf(frame(1, 5.0), frame(2, 6.0), frame(3, 7.0))).id)
    }

    @Test
    fun excludedFramesWithNegativeScoresAreNotRevived() {
        val picked = KeyframeTie.best(listOf(frame(1, -1.0), frame(2, -1.0, key = true)))
        assertEquals(1L, picked.id)
    }

    @Test
    fun pickerUsesTheTieBreakPerWindow() {
        val frames = (0 until 10).map { frame(it.toLong(), if (it == 3) 50.0 else if (it == 4) 49.8 else 1.0, key = it == 4) }
        assertEquals(listOf(4L), SharpestPicker.pick(frames).filter { it == 3L || it == 4L })
    }
}
