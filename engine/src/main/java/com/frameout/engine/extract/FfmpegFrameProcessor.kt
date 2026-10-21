package com.frameout.engine.extract

import android.graphics.Bitmap
import com.frameout.codecpack.MediaFailureException
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.VideoIndexDao
import com.frameout.core.quality.ExtractQualityGate
import com.frameout.core.storage.StorageManager
import com.frameout.engine.decode.FfmpegDecodedFrame

internal class FfmpegFrameProcessor(
    private val storage: StorageManager,
    private val videoIndexDao: VideoIndexDao,
    private val gate: ExtractQualityGate?
) {
    private var consecutiveFailures = 0

    suspend fun prepare(
        request: ExtractRequest,
        session: FfmpegSession,
        index: Int,
        previousHistogram: FloatArray?
    ): FfmpegStaged {
        val decoded = decode(request, session, index)
        if (decoded == null) {
            val reason = session.router.lastFfmpegFailure()
            if (reason.fatal || ++consecutiveFailures > MAX_CONSECUTIVE_FAILURES) {
                throw MediaFailureException(reason, "FFmpeg frame decode failed at index $index")
            }
            return FfmpegStaged.Idle(index, null, false)
        }
        consecutiveFailures = 0
        val pts = FfmpegFrameRules.resolvePts(decoded.ptsUs, index)
        if (!FfmpegFrameRules.inRange(pts, session.startUs, session.endUs)) {
            decoded.bitmap.recycle()
            return FfmpegStaged.Idle(index, null, false)
        }
        val histogram = SceneChangeSelector.signature(request.mode, decoded.bitmap) ?: previousHistogram
        val keep = FfmpegFrameRules.keep(request.mode, index, pts, decoded.isKeyframe, previousHistogram, histogram)
        if (!keep || gate?.acceptAsync(decoded.bitmap) == false) {
            decoded.bitmap.recycle()
            return FfmpegStaged.Idle(index, SceneChangeSelector.carry(request.mode, previousHistogram, histogram), true)
        }
        return FfmpegStaged.Frame(index, pts, decoded.bitmap, decoded.isKeyframe, histogram)
    }

    suspend fun write(request: ExtractRequest, staged: FfmpegStaged.Frame): FrameEntity {
        val bitmap = staged.bitmap
        try {
            val thumbPath = writeThumb(request, staged.index, bitmap)
            val fullPath = if (request.autoSave) {
                storage.writeFrame(request.projectId, staged.index, bitmap, request.saveFormat, request.jpegQuality)
            } else {
                null
            }
            AnalysisCopy.write(storage, request, staged.index, bitmap)
            return FrameEntity(projectId = request.projectId, index = staged.index, ptsUs = staged.pts, isKeyframe = staged.keyframe, thumbnailPath = thumbPath, fullPath = fullPath)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private suspend fun decode(request: ExtractRequest, session: FfmpegSession, index: Int): FfmpegDecodedFrame? {
        val router = session.router
        if (!router.wantsKeyframeHint(index)) return router.decodeFfmpegFrame(index)
        val entry = videoIndexDao.bySampleIndex(request.projectId, index)
        val hintIndex = entry?.nearestKeyframeIndex ?: -1
        val hintPts = entry?.nearestKeyframePtsUs ?: -1L
        return router.decodeFfmpegFrame(index, hintIndex, hintPts)
    }

    private suspend fun writeThumb(request: ExtractRequest, index: Int, bitmap: Bitmap): String {
        val thumb = ffmpegThumb(bitmap)
        try {
            return storage.writeThumb(request.projectId, index, thumb)
        } finally {
            if (thumb !== bitmap) thumb.recycle()
        }
    }

    private companion object {
        const val MAX_CONSECUTIVE_FAILURES = 32
    }
}
