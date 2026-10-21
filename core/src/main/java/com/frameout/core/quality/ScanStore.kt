package com.frameout.core.quality

import com.frameout.core.db.FrameEntity
import com.frameout.core.db.FrameScanEntity
import com.frameout.core.db.FrameTagDao
import com.frameout.core.db.FrameTagEntity

interface ScanStore {
    suspend fun countUnscanned(projectId: Long): Int
    suspend fun unscanned(projectId: Long, limit: Int): List<FrameEntity>
    suspend fun commit(frameIds: List<Long>, tags: List<FrameTagEntity>, scans: List<FrameScanEntity>)
}

class DaoScanStore(private val dao: FrameTagDao) : ScanStore {
    override suspend fun countUnscanned(projectId: Long): Int = dao.countUnscanned(projectId)
    override suspend fun unscanned(projectId: Long, limit: Int): List<FrameEntity> = dao.unscanned(projectId, limit)
    override suspend fun commit(frameIds: List<Long>, tags: List<FrameTagEntity>, scans: List<FrameScanEntity>) = dao.commitScan(frameIds, tags, scans)
}

class FrameAnalysis(val tags: List<FrameTagEntity>, val failed: List<String>)

interface FrameAnalyzer {
    suspend fun analyze(frame: FrameEntity, projectId: Long): FrameAnalysis?
}

class FrameUnreadable(path: String) : Exception("frame could not be decoded: $path")
