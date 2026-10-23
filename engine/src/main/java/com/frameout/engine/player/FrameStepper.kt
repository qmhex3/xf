package com.frameout.engine.player

import com.frameout.core.util.logged
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.frameout.codecpack.CodecPack
import com.frameout.codecpack.FormatKind
import com.frameout.codecpack.FormatProbe
import com.frameout.core.db.AppDatabase
import com.frameout.engine.decode.DecodeRouter

object FrameStepper {
    const val JUMP_FRAMES = 10

    suspend fun step(context: Context, uri: Uri, currentMs: Long, forward: Boolean, count: Int = 1): Long? {
        val frames = count.coerceAtLeast(1)
        return if (FormatProbe.probe(context, uri).kind == FormatKind.NeedsFfmpeg) {
            stepFfmpeg(context, uri, currentMs, forward, frames)
        } else {
            SystemStepper.step(context, uri, currentMs, forward, frames)
        }
    }

    private suspend fun stepFfmpeg(context: Context, uri: Uri, currentMs: Long, forward: Boolean, count: Int): Long? = runCatching {
        if (!CodecPack.load(context)) return null
        val projectId = findProjectId(context, uri) ?: return null
        val dao = AppDatabase.get(context).videoIndexDao()
        var positionUs = if (forward) ExactPts.limitUs(currentMs) else currentMs.coerceAtLeast(0L) * 1000L
        var moved = false
        repeat(count) {
            val row = if (forward) dao.atOrAfter(projectId, positionUs + 1L) else dao.atOrBefore(projectId, positionUs - 1L)
            if (row != null) {
                positionUs = row.ptsUs
                moved = true
            }
        }
        if (moved) positionUs / 1000L else null
    }.logged("FrameStepper").getOrNull()

    suspend fun capture(context: Context, uri: Uri, positionMs: Long): Bitmap? = runCatching {
        if (FormatProbe.probe(context, uri).kind == FormatKind.NeedsFfmpeg) return captureFfmpeg(context, uri, positionMs)
        val ptsUs = ExactPts.displayedAt(context, uri, positionMs) ?: (positionMs.coerceAtLeast(0L) * 1000L)
        android.media.MediaMetadataRetriever().let { retriever ->
            try { retriever.setDataSource(context, uri); retriever.getFrameAtTime(ptsUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST) }
            finally { retriever.release() }
        }
    }.logged("FrameStepper").getOrNull()

    private suspend fun captureFfmpeg(context: Context, uri: Uri, positionMs: Long): Bitmap? {
        if (!CodecPack.load(context)) return null
        val projectId = findProjectId(context, uri) ?: return null
        val dao = AppDatabase.get(context).videoIndexDao()
        val targetUs = positionMs.coerceAtLeast(0L) * 1000L
        val item = dao.atOrBefore(projectId, ExactPts.limitUs(positionMs)) ?: dao.atOrAfter(projectId, targetUs) ?: return null
        val router = DecodeRouter(context)
        return try {
            router.open(uri)
            if (item.nearestKeyframeIndex >= 0) router.seekFfmpegToKeyframe(item.nearestKeyframeIndex, item.nearestKeyframePtsUs)
            router.decodeFfmpegFrame(item.sampleIndex)?.bitmap
        } finally { router.close() }
    }

    private suspend fun findProjectId(context: Context, uri: Uri): Long? {
        return AppDatabase.get(context).projectDao().getBySourceUri(uri.toString())?.id
    }


}
