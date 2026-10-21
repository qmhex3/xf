package com.frameout.engine.extract

import com.frameout.core.db.VideoIndexDao
import com.frameout.core.db.VideoIndexEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal object FixedSelection {
    private const val PAGE_SIZE = 500

    suspend fun count(dao: VideoIndexDao, projectId: Long, startUs: Long, endUs: Long, requested: Int): Int =
        requested.coerceIn(0, dao.countRange(projectId, startUs, endUs))

    fun offset(i: Int, rangeCount: Int, count: Int): Long =
        if (count <= 1) 0L else i.toLong() * (rangeCount - 1) / (count - 1)

    fun positions(rangeCount: Int, requested: Int, ptsAt: (Int) -> Long): IntArray {
        val count = requested.coerceIn(0, rangeCount)
        if (count == 0) return IntArray(0)
        if (count == 1) return IntArray(1)
        val first = ptsAt(0)
        val span = ptsAt(rangeCount - 1) - first
        if (span <= 0L) return IntArray(count) { offset(it, rangeCount, count).toInt() }
        val out = IntArray(count)
        var cursor = 0
        var previous = -1
        for (i in 0 until count) {
            val target = first + span * i / (count - 1)
            while (cursor + 1 < rangeCount && ptsAt(cursor + 1) <= target) cursor++
            val pick = cursor.coerceIn(previous + 1, rangeCount - (count - i))
            out[i] = pick
            previous = pick
        }
        return out
    }

    fun flow(dao: VideoIndexDao, projectId: Long, startUs: Long, endUs: Long, requested: Int): Flow<VideoIndexEntity> = flow {
        val rangeCount = dao.countRange(projectId, startUs, endUs)
        if (requested.coerceIn(0, rangeCount) == 0) return@flow
        val points = collectPts(dao, projectId, startUs, endUs, rangeCount)
        val picks = positions(points.size, requested) { points[it] }
        var next = 0
        var position = 0
        var after = -1
        while (next < picks.size) {
            val page = if (after >= 0) dao.pageRangeAfter(projectId, startUs, endUs, after, PAGE_SIZE) else dao.pageRange(projectId, startUs, endUs, PAGE_SIZE, 0)
            if (page.isEmpty()) break
            for (entity in page) {
                while (next < picks.size && picks[next] == position) {
                    emit(entity)
                    next++
                }
                position++
            }
            after = page.last().sampleIndex
            if (page.size < PAGE_SIZE) break
        }
    }

    private suspend fun collectPts(dao: VideoIndexDao, projectId: Long, startUs: Long, endUs: Long, expected: Int): LongArray {
        var out = LongArray(expected.coerceAtLeast(1))
        var size = 0
        var after = -1
        while (true) {
            val page = if (after >= 0) dao.pageRangeAfter(projectId, startUs, endUs, after, PAGE_SIZE) else dao.pageRange(projectId, startUs, endUs, PAGE_SIZE, 0)
            if (page.isEmpty()) break
            for (entity in page) {
                if (size == out.size) out = out.copyOf(size * 2)
                out[size++] = entity.ptsUs
            }
            after = page.last().sampleIndex
            if (page.size < PAGE_SIZE) break
        }
        return out.copyOf(size)
    }
}
