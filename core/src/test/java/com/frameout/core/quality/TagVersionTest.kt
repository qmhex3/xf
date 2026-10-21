package com.frameout.core.quality

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagVersionTest {
    private class Memory(var value: Int) : TagVersionStore {
        override fun stored(): Int = value

        override fun save(version: Int) {
            value = version
        }
    }

    @Test
    fun olderStoredVersionResetsOnceAndIsSaved() = runBlocking {
        val store = Memory(1)
        var resets = 0
        assertTrue(TagVersion.ensure(store, 2) { resets++ })
        assertEquals(2, store.value)
        assertFalse(TagVersion.ensure(store, 2) { resets++ })
        assertEquals(1, resets)
    }

    @Test
    fun freshInstallResetsOnce() = runBlocking {
        val store = Memory(0)
        var resets = 0
        TagVersion.ensure(store, 2) { resets++ }
        TagVersion.ensure(store, 2) { resets++ }
        assertEquals(1, resets)
    }

    @Test
    fun failedResetKeepsTheOldVersionSoItRetries() = runBlocking {
        val store = Memory(1)
        try {
            TagVersion.ensure(store, 2) { error("db down") }
        } catch (e: IllegalStateException) {
            assertEquals(1, store.value)
        }
        var resets = 0
        assertTrue(TagVersion.ensure(store, 2) { resets++ })
        assertEquals(1, resets)
    }

    @Test
    fun newerStoredVersionIsLeftAlone() = runBlocking {
        val store = Memory(5)
        var resets = 0
        assertFalse(TagVersion.ensure(store, 2) { resets++ })
        assertEquals(0, resets)
        assertEquals(5, store.value)
    }

    @Test
    fun taggerVersionIsAtLeastTwo() {
        assertTrue(FrameTagger.VERSION >= 2)
    }
}
