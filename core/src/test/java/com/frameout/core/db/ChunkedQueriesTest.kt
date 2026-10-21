package com.frameout.core.db

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkedQueriesTest {
    private class Call(val name: String, val ids: List<Long>, val flag: Boolean?)

    private val calls = ArrayList<Call>()

    private fun frame(id: Long) = FrameEntity(id = id, projectId = 1, index = (10_000 - id).toInt(), ptsUs = id)

    @Suppress("UNCHECKED_CAST")
    private fun frameDao(): FrameDao = Proxy.newProxyInstance(FrameDao::class.java.classLoader, arrayOf(FrameDao::class.java), InvocationHandler { _, method, args ->
        val ids = args[0] as List<Long>
        calls += Call(method.name, ids, args.getOrNull(1) as? Boolean)
        if (method.name == "getByIds") ids.map(::frame) else Unit
    }) as FrameDao

    @Suppress("UNCHECKED_CAST")
    private fun savedDao(): SavedImageDao = Proxy.newProxyInstance(SavedImageDao::class.java.classLoader, arrayOf(SavedImageDao::class.java), InvocationHandler { _, method, args ->
        val ids = args[0] as List<Long>
        calls += Call(method.name, ids, null)
        if (method.name == "getByIds") ids.map { SavedImageEntity(id = it, uri = "u$it", displayName = "n$it", createdAt = it) } else Unit
    }) as SavedImageDao

    private fun ids(n: Int) = (1..n).map { it.toLong() }

    @Test
    fun largeSelectionIsSplitUnderTheSqliteVariableLimit() = runBlocking {
        val result = frameDao().getByIdsChunked(ids(3000))
        assertEquals(3000, result.size)
        assertEquals(6, calls.size)
        assertTrue(calls.all { it.ids.size <= SQL_IN_CHUNK })
        assertTrue(SQL_IN_CHUNK < 999)
    }

    @Test
    fun resultIsOrderedByFrameIndexAcrossChunks() = runBlocking {
        val result = frameDao().getByIdsChunked(ids(1200))
        assertEquals(result.map { it.index }, result.map { it.index }.sorted())
        assertEquals(1200L, result.first().id)
    }

    @Test
    fun smallSelectionUsesOneQueryAndEmptyUsesNone() = runBlocking {
        assertEquals(10, frameDao().getByIdsChunked(ids(10)).size)
        assertEquals(1, calls.size)
        calls.clear()
        assertTrue(frameDao().getByIdsChunked(emptyList()).isEmpty())
        assertTrue(calls.isEmpty())
    }

    @Test
    fun duplicateIdsAreQueriedOnce() = runBlocking {
        val result = frameDao().getByIdsChunked(listOf(5L, 5L, 6L, 5L))
        assertEquals(listOf(6L, 5L), result.map { it.id })
        assertEquals(2, calls.single().ids.size)
    }

    @Test
    fun writesAreChunkedAndKeepTheirArguments() = runBlocking {
        val dao = frameDao()
        dao.softDeleteChunked(ids(1001))
        dao.restoreChunked(ids(501))
        dao.permanentDeleteChunked(ids(500))
        dao.setFavoriteManyChunked(ids(1000), true)
        assertEquals(listOf(500, 500, 1), calls.filter { it.name == "softDelete" }.map { it.ids.size })
        assertEquals(listOf(500, 1), calls.filter { it.name == "restore" }.map { it.ids.size })
        assertEquals(listOf(500), calls.filter { it.name == "permanentDelete" }.map { it.ids.size })
        val favorites = calls.filter { it.name == "setFavoriteMany" }
        assertEquals(2, favorites.size)
        assertTrue(favorites.all { it.flag == true })
    }

    @Test
    fun savedImagesAreChunkedForReadAndDelete() = runBlocking {
        val dao = savedDao()
        assertEquals(1700, dao.getByIdsChunked(ids(1700)).size)
        dao.deleteManyChunked(ids(1700))
        assertEquals(4, calls.count { it.name == "getByIds" })
        assertEquals(4, calls.count { it.name == "deleteMany" })
        assertTrue(calls.all { it.ids.size <= SQL_IN_CHUNK })
    }
}
