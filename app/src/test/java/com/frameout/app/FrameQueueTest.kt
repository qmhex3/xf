package com.frameout.app

import com.frameout.app.trim.FrameQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameQueueTest {
    @Test
    fun toggleAddsSortedAndRemoves() {
        val added = FrameQueue.toggle(FrameQueue.toggle(emptyList(), 500L), 100L)
        assertEquals(listOf(100L, 500L), added)
        assertEquals(listOf(500L), FrameQueue.toggle(added, 100L))
    }

    @Test
    fun containsReflectsMembership() {
        assertTrue(FrameQueue.contains(listOf(10L), 10L))
        assertFalse(FrameQueue.contains(listOf(10L), 11L))
    }

    @Test
    fun capsAtMax() {
        val full = (1L..FrameQueue.MAX).toList()
        assertEquals(full, FrameQueue.toggle(full, 9999L))
    }
}
