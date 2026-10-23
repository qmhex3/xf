package com.frameout.engine.extract

import com.frameout.core.db.VideoIndexDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

object FfmpegIndexedSelection {
    suspend fun selectedCount(dao: VideoIndexDao, projectId: Long, mode: ExtractMode, startUs: Long, endUs: Long, checkpointIndex: Int): Int =
        source(dao, projectId).selectedCount(mode, bounds(startUs, endUs), checkpointPts(dao, projectId, mode, checkpointIndex), checkpointIndex)

    fun flow(dao: VideoIndexDao, projectId: Long, mode: ExtractMode, startUs: Long, endUs: Long, checkpointIndex: Int): Flow<Int> = flow {
        val checkpoint = checkpointPts(dao, projectId, mode, checkpointIndex)
        emitAll(source(dao, projectId).sampleFlow(mode, bounds(startUs, endUs), checkpoint, checkpointIndex).map { it.index })
    }

    private suspend fun checkpointPts(dao: VideoIndexDao, projectId: Long, mode: ExtractMode, checkpointIndex: Int): Long {
        if ((mode !is ExtractMode.EveryNMillis && mode !is ExtractMode.Content) || checkpointIndex < 0) return -1L
        return dao.bySampleIndex(projectId, checkpointIndex)?.ptsUs ?: -1L
    }

    private fun bounds(startUs: Long, endUs: Long) = IndexSampleSource.Bounds(startUs, maxOf(endUs, startUs + 1L))

    private fun source(dao: VideoIndexDao, projectId: Long) = IndexSampleSource(dao, projectId, 0L)
}
