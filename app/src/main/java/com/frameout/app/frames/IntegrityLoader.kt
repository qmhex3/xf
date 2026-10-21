package com.frameout.app.frames

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.frameout.app.FrameOutApp
import com.frameout.core.db.ProjectEntity
import com.frameout.core.quality.FrameIntegrity
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractModeCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object IntegrityLoader {
    suspend fun load(context: Context, project: ProjectEntity): FrameIntegrity.Report? = withContext(Dispatchers.IO) {
        if (project.status != "Completed") return@withContext null
        val container = (context.applicationContext as FrameOutApp).container
        val pts = container.frames.listPts(project.id)
        if (pts.isEmpty()) return@withContext null
        val mode = ExtractModeCodec.decode(project.extractMode)
        val total = project.frameCount
        val fullRange = project.startMs <= 0L && (project.endMs <= 0L || project.endMs >= project.durationMs)
        val expected = if (!fullRange || total <= 0) null else when (mode) {
            is ExtractMode.EveryFrame -> total
            is ExtractMode.EveryNFrames -> (total + mode.n - 1) / mode.n
            is ExtractMode.FixedCount -> minOf(mode.count, total)
            else -> null
        }
        val stride = (mode as? ExtractMode.EveryNFrames)?.n ?: 1
        val rateCheck = mode is ExtractMode.EveryFrame || mode is ExtractMode.EveryNFrames
        FrameIntegrity.analyze(
            FrameIntegrity.Input(
                expected = expected,
                actual = pts.size,
                ptsUs = pts,
                durationMs = project.durationMs,
                rateCheck = rateCheck,
                tailCheck = fullRange && mode is ExtractMode.EveryFrame,
                stride = stride,
                captureFps = if (rateCheck) captureFps(context, project.sourceUri) else null
            )
        )
    }

    private fun captureFps(context: Context, sourceUri: String): Double? {
        if (sourceUri.isBlank()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(sourceUri))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toDoubleOrNull()?.takeIf { it > 0.0 }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
