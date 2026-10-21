package com.frameout.app

import com.frameout.app.viewer.WindowCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowCacheTest {
    private val dropped = mutableListOf<String>()
    private fun cache(budget: Long = 30) = WindowCache<String>(budget, 20, { 10L }, { dropped += it })

    @Test
    fun takeReturnsMatchingEntryAndRemovesIt() {
        val c = cache().apply { setWindow(listOf(1, 2)) }
        c.put(1, "e", "a")
        assertEquals("a", c.take(1, "e"))
        assertNull(c.take(1, "e"))
        assertEquals(0L, c.usedBytes)
    }

    @Test
    fun staleEditIsDropped() {
        val c = cache().apply { setWindow(listOf(1)) }
        c.put(1, "old", "a")
        assertNull(c.take(1, "new"))
        assertEquals(listOf("a"), dropped)
    }

    @Test
    fun leavingWindowDropsEntries() {
        val c = cache().apply { setWindow(listOf(1, 2, 3)) }
        listOf(1L, 2L, 3L).forEach { c.put(it, null, "f$it") }
        c.setWindow(listOf(3, 4))
        assertFalse(c.contains(1)); assertFalse(c.contains(2)); assertTrue(c.contains(3))
        assertEquals(listOf("f1", "f2"), dropped)
    }

    @Test
    fun overBudgetEvictsLeastLikelyFirst() {
        val c = cache(30).apply { setWindow(listOf(1, 2, 3, 4)) }
        c.put(2, null, "b"); c.put(3, null, "c"); c.put(4, null, "d")
        assertTrue(c.put(1, null, "a"))
        assertFalse(c.contains(4))
        assertEquals(listOf("d"), dropped)
    }

    @Test
    fun lowPriorityEntryCannotEvictBetterOne() {
        val c = cache(20).apply { setWindow(listOf(1, 2, 3)) }
        c.put(1, null, "a"); c.put(2, null, "b")
        assertFalse(c.put(3, null, "c"))
        assertTrue(c.contains(1) && c.contains(2))
    }

    @Test
    fun oversizedEntryIsRejected() {
        val c = WindowCache<String>(100, 5, { 10L }, { dropped += it }).apply { setWindow(listOf(1)) }
        assertFalse(c.put(1, null, "big"))
        assertEquals(listOf("big"), dropped)
    }
}
