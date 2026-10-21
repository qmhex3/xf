package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Test

class SlideDeckTest {
    private fun flat(value: Float) = FloatArray(SlideSignature.COLS * SlideSignature.ROWS) { value }

    private fun patch(base: Float, other: Float, share: Float): FloatArray {
        val size = SlideSignature.COLS * SlideSignature.ROWS
        val changed = (size * share).toInt()
        return FloatArray(size) { if (it < changed) other else base }
    }

    private fun frame(id: Long, seconds: Double, signature: FloatArray, score: Double = 1.0) =
        SlideCandidate(id, (seconds * 1_000_000).toLong(), signature, score)

    @Test
    fun revisitedSlideKeepsOnlyTheSharpestFrame() {
        val a = flat(0.9f)
        val b = patch(0.9f, 0.1f, 0.3f)
        val ids = SlideDeck.unique(
            listOf(frame(1, 0.0, a, 1.0), frame(2, 5.0, b, 1.0), frame(3, 10.0, a, 5.0)),
            false
        )
        assertEquals(listOf(3L, 2L), ids)
    }

    @Test
    fun smallCursorMovementDoesNotCreateNewSlide() {
        val a = flat(0.9f)
        val cursor = patch(0.9f, 0.5f, 0.01f)
        val ids = SlideDeck.unique(listOf(frame(1, 0.0, a, 1.0), frame(2, 4.0, cursor, 2.0)), false)
        assertEquals(listOf(2L), ids)
    }

    @Test
    fun shortLivedTransitionFramesAreDropped() {
        val a = flat(0.9f)
        val fade = patch(0.9f, 0.5f, 0.5f)
        val b = patch(0.9f, 0.1f, 0.5f)
        val frames = listOf(frame(1, 0.0, a), frame(2, 6.0, fade), frame(3, 6.25, b))
        assertEquals(listOf(1L, 3L), SlideDeck.unique(frames, true))
        assertEquals(listOf(1L, 2L, 3L), SlideDeck.unique(frames, false))
    }

    @Test
    fun singleFrameIsKept() {
        assertEquals(listOf(7L), SlideDeck.unique(listOf(frame(7, 0.0, flat(0.5f))), true))
    }
}
