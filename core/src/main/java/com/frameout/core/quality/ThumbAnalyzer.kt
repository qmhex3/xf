package com.frameout.core.quality

import com.frameout.core.db.FrameEntity
import com.frameout.core.db.FrameTagEntity
import com.frameout.core.util.DeviceLimits
import com.frameout.core.util.ThumbCache

internal object ThumbAnalyzer : FrameAnalyzer {
    override suspend fun analyze(frame: FrameEntity, projectId: Long): FrameAnalysis? {
        val key = frame.thumbnailPath ?: frame.fullPath ?: return null
        FaceScanner.assessFrame(frame)
        val paths = AnalysisSource.candidates(frame.fullPath, frame.thumbnailPath, frame.index)
        val bitmap = AnalysisBitmap.load(paths, AnalysisSource.side(DeviceLimits.analysisReduced())) ?: throw FrameUnreadable(key)
        try {
            val outcome = FrameTagger.tagDetailed(key, bitmap)
            val tags = outcome.hits.map {
                FrameTagEntity(frameId = frame.id, projectId = projectId, tag = it.tag, confidence = it.confidence, boxes = NormBoxCodec.encode(it.boxes))
            }
            return FrameAnalysis(tags, outcome.failed)
        } finally {
            ThumbCache.release(bitmap)
        }
    }
}
