package com.frameout.app.frames

import android.app.Application
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectEntity
import com.frameout.core.util.FrameStrings
import com.frameout.engine.edit.KeptRange
import com.frameout.engine.edit.ReassemblePlan
import com.frameout.engine.edit.ReassembleResult
import com.frameout.engine.edit.VideoReassembler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FrameReassembler {
    @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
    suspend fun reassemble(app: Application, project: ProjectEntity, frames: List<FrameEntity>): ReassembleResult = withContext(Dispatchers.IO) {
        val ranges = buildRanges(frames)
        if (ranges.isEmpty()) return@withContext ReassembleResult.Failure("No frames kept")
        val out = java.io.File(app.cacheDir, "export/project_${project.id}_edit.mp4")
        VideoReassembler(app).reassemble(ReassemblePlan(Uri.parse(project.sourceUri).toString(), out.absolutePath, ranges))
    }

    suspend fun summary(app: Application, project: ProjectEntity, frames: List<FrameEntity>): String {
        val result = try {
            reassemble(app, project, frames)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            com.frameout.core.util.logFailure("FrameReassembler", e)
            null
        }
        return when (result) {
            is ReassembleResult.Success -> FrameStrings.exportedMb("%.1f".format(result.sizeBytes / 1048576.0)) + if (result.reencoded) " · ${FrameStrings.reencodedWarning()}" else ""
            else -> FrameStrings.reassembleFailed()
        }
    }

    internal fun buildRanges(frames: List<FrameEntity>): List<KeptRange> {
        val ordered = frames.sortedBy { it.index }
        if (ordered.isEmpty()) return emptyList()
        val duration = medianGap(ordered.map { it.ptsUs }).coerceAtLeast(1L)
        val ranges = mutableListOf<KeptRange>()
        var start = ordered.first().ptsUs
        var previousIndex = ordered.first().index
        var lastPts = start
        for (frame in ordered.drop(1)) {
            if (frame.index != previousIndex + 1) {
                ranges += KeptRange(start, lastPts + duration)
                start = frame.ptsUs
            }
            previousIndex = frame.index
            lastPts = frame.ptsUs
        }
        ranges += KeptRange(start, lastPts + duration)
        return ranges
    }

    private fun medianGap(pts: List<Long>): Long {
        if (pts.size < 2) return 33_333L
        val gaps = pts.zipWithNext { a, b -> (b - a).coerceAtLeast(1L) }.sorted()
        return gaps[gaps.size / 2]
    }
}
