package com.frameout.engine.extract

import android.content.Context
import android.net.Uri
import com.frameout.codecpack.FfmpegBridge
import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureException
import com.frameout.core.db.AppDatabase
import com.frameout.core.db.FrameDao
import com.frameout.core.db.ProjectDao
import com.frameout.core.db.ProjectEntity
import com.frameout.core.db.VideoIndexDao
import com.frameout.core.storage.StorageManager
import com.frameout.engine.decode.DecodeRouter
import com.frameout.engine.index.FfmpegIndexManager

internal class FfmpegSessionPlanner(
    private val context: Context,
    private val projectDao: ProjectDao,
    private val frameDao: FrameDao,
    private val storage: StorageManager,
    private val videoIndexDao: VideoIndexDao,
    private val shouldAbort: () -> Boolean
) {
    private companion object {
        const val TABLE_PAGE = 2000
    }

    private class Resume(val existing: Int, val checkpoint: Long, val index: Int)

    suspend fun plan(request: ExtractRequest, uri: Uri, router: DecodeRouter, resumeByPts: Boolean = false): FfmpegSession {
        router.open(uri)
        val project = projectDao.getById(request.projectId)
        val durationMs = project?.durationMs?.takeIf { it > 0L } ?: ffmpegDurationMs(context, uri)
        val index = openIndex(request, project, router, durationMs)
        val view = index.state.value
        recordIndexMeta(request, durationMs, if (view.complete) view.count else 0)
        val resume = resumeState(request, project, resumeByPts)
        seekToResume(request, router, resume)
        return buildSession(request, router, resume, index, durationMs)
    }

    private suspend fun openIndex(request: ExtractRequest, project: ProjectEntity?, router: DecodeRouter, durationMs: Long): StagedIndex {
        val path = router.nativePath() ?: throw MediaFailureException(MediaFailure.IoError, "FFmpeg needs local path")
        val staged = StagedIndex(FfmpegIndexManager(AppDatabase.get(context)), request.projectId, path, shouldAbort) { total ->
            recordIndexMeta(request, durationMs, total)
        }
        val storedCount = videoIndexDao.count(request.projectId)
        if (project?.frameCount == storedCount && storedCount > 0) {
            val table = tableFromIndex(request.projectId)
            if (table.isNotEmpty()) FfmpegBridge.nativeSetSessionTable(path, table)
            staged.markComplete(table, storedCount)
            return staged
        }
        val early = request.mode !is ExtractMode.FixedCount && staged.first()
        if (!early) staged.finish()
        return staged
    }

    private suspend fun tableFromIndex(projectId: Long): LongArray {
        val total = videoIndexDao.count(projectId)
        if (total <= 0) return LongArray(0)
        val out = LongArray(total * 4)
        var filled = 0
        var after = -1
        while (filled < total) {
            val page = videoIndexDao.pageAfter(projectId, after, TABLE_PAGE)
            if (page.isEmpty()) break
            for (entry in page) {
                if (filled >= total) break
                val at = filled * 4
                out[at] = entry.ptsUs
                out[at + 1] = if (entry.isKeyframe) 1L else 0L
                out[at + 2] = entry.nearestKeyframeIndex.toLong()
                out[at + 3] = entry.nearestKeyframePtsUs
                filled++
            }
            after = page.last().sampleIndex
        }
        return if (filled == total) out else out.copyOf(filled * 4)
    }

    private suspend fun recordIndexMeta(request: ExtractRequest, durationMs: Long, total: Int) {
        projectDao.updateIndexMeta(
            request.projectId,
            durationMs,
            total,
            FfmpegSelection.encodeMode(request.mode),
            request.autoSave,
            request.saveFormat,
            request.jpegQuality,
            request.startMs,
            request.endMs,
            System.currentTimeMillis()
        )
    }

    private suspend fun resumeState(request: ExtractRequest, project: ProjectEntity?, byPts: Boolean): Resume {
        val existing = frameDao.countAll(request.projectId)
        if (existing <= 0) return Resume(existing, -1L, -1)
        val checkpoint = project?.lastPtsUs ?: 0L
        if (byPts) {
            val entry = videoIndexDao.atOrBefore(request.projectId, checkpoint)
            return Resume(existing, checkpoint, entry?.sampleIndex ?: -1)
        }
        return Resume(existing, checkpoint, project?.lastSampleIndex ?: -1)
    }

    private suspend fun seekToResume(request: ExtractRequest, router: DecodeRouter, resume: Resume) {
        if (resume.index < 0) return
        val entry = videoIndexDao.bySampleIndex(request.projectId, resume.index) ?: return
        if (entry.nearestKeyframeIndex >= 0 && entry.nearestKeyframePtsUs >= 0) {
            router.seekFfmpegToKeyframe(entry.nearestKeyframeIndex, entry.nearestKeyframePtsUs)
        }
    }

    private suspend fun buildSession(request: ExtractRequest, router: DecodeRouter, resume: Resume, index: StagedIndex, durationMs: Long): FfmpegSession {
        val startUs = request.startMs.coerceAtLeast(0) * 1000L
        val endUs = if (request.endMs > 0) request.endMs * 1000L else Long.MAX_VALUE
        val durationUs = durationMs * 1000L
        val dao = videoIndexDao
        val count: suspend (Int) -> Int = { cursor -> FfmpegIndexedSelection.selectedCount(dao, request.projectId, request.mode, startUs, endUs, cursor) }
        val estimate = scaleCandidates(count(resume.index), index.state.value, startUs, endUs, durationUs)
        val hasSpace = storage.hasSpaceForResolution(
            SpaceEstimate.frames(request.mode, estimate),
            router.ffmpegWidth(),
            router.ffmpegHeight(),
            request.autoSave,
            request.saveFormat
        )
        if (!hasSpace) throw LowSpaceException()
        val select = { cursor: Int -> FfmpegIndexedSelection.flow(dao, request.projectId, request.mode, startUs, endUs, cursor) }
        return FfmpegSession(
            router, index, select, count, startUs, endUs, resume.checkpoint,
            resume.existing + estimate, resume.existing, resume.checkpoint, resume.index, resume.index, durationUs
        )
    }
}
