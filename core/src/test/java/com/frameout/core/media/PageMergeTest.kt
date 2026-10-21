package com.frameout.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PageMergeTest {
    private fun key(date: Long, id: Long) = MediaKey(date, id)

    private fun queryStore(store: List<MediaKey>, cursor: MediaKey?, limit: Int): List<MediaKey> =
        store.filter { cursor == null || it < cursor }.take(limit)

    private fun pageThrough(store: List<MediaKey>, folder: List<MediaKey>, limit: Int): List<MediaKey> {
        val out = ArrayList<MediaKey>()
        var cursor: MediaKey? = null
        var pages = 0
        while (true) {
            val raw = queryStore(store, cursor, limit)
            val plan = PageMerge.plan(raw.lastOrNull(), raw.size >= limit, folder, cursor, limit) { it }
            out += (raw.filter { plan.accepts(it) } + plan.folder).sortedDescending()
            pages++
            assertTrue(pages < 5000)
            val next = plan.next ?: break
            assertTrue(cursor == null || next < cursor)
            cursor = next
        }
        return out
    }

    @Test
    fun randomizedPagingNeverDuplicatesOrDropsItems() {
        repeat(3000) { seed ->
            val random = Random(seed)
            val limit = listOf(1, 2, 3, 5, 8, 60).random(random)
            val dates = random.nextInt(1, 15)
            val storeIds = (1L..10_000L).shuffled(random).take(random.nextInt(0, 120))
            val folderIds = (-10_000L..-1L).shuffled(random).take(random.nextInt(0, 120))
            val store = storeIds.map { key(random.nextLong(1, dates + 1L), it) }.sortedDescending()
            val folder = folderIds.map { key(random.nextLong(1, dates + 1L), it) }.sortedDescending()
            val result = pageThrough(store, folder, limit)
            assertEquals("seed $seed", (store + folder).sortedDescending(), result)
            assertEquals(result.size, result.toSet().size)
        }
    }

    @Test
    fun emptySourcesProduceOneEmptyFinalPage() {
        val plan = PageMerge.plan<MediaKey>(null, false, emptyList(), null, 60) { it }
        assertTrue(plan.folder.isEmpty())
        assertEquals(null, plan.next)
    }

    @Test
    fun folderOnlyListingPagesByLimit() {
        val folder = (1L..10L).map { key(100L - it, -it) }
        val first = PageMerge.plan(null, false, folder, null, 4) { it }
        assertEquals(4, first.folder.size)
        assertEquals(folder[3], first.next)
        val last = PageMerge.plan(null, false, folder, folder[7], 4) { it }
        assertEquals(2, last.folder.size)
        assertEquals(null, last.next)
    }

    @Test
    fun folderItemsBeyondStoreWindowAreDeferred() {
        val store = listOf(key(50, 5), key(40, 4))
        val folder = listOf(key(45, -1), key(10, -2))
        val plan = PageMerge.plan(store.last(), true, folder, null, 2) { it }
        assertEquals(listOf(folder[0]), plan.folder)
        assertEquals(store.last(), plan.next)
    }
}
