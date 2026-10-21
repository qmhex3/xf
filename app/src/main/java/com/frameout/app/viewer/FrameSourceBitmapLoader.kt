package com.frameout.app.viewer

import com.frameout.core.util.SmoothScale
import com.frameout.core.util.logged
import com.frameout.core.util.logFailure
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.frameout.codecpack.FormatKind
import com.frameout.codecpack.FormatProbe
import com.frameout.core.db.AppDatabase
import com.frameout.engine.decode.DecodeRouter
import com.frameout.engine.decode.FrameDecoder
import com.frameout.engine.decode.FramePuller
import com.frameout.engine.decode.AnimatedFrameSource
import com.frameout.engine.decode.ApngFrameDecoder
import com.frameout.engine.decode.GifFrameDecoder
import com.frameout.engine.decode.WebpFrameDecoder

class FrameSourceBitmapLoader(private val context: Context) {
    suspend fun decodeAtPts(projectId: Long, uri: Uri, ptsUs: Long): Bitmap? {
        return when (FormatProbe.probe(context, uri).kind) {
            FormatKind.NeedsFfmpeg -> decodeFfmpeg(projectId, uri, ptsUs)
            FormatKind.Gif -> decodeAnimated(uri, ptsUs) { GifFrameDecoder(context) }
            FormatKind.Apng -> decodeAnimated(uri, ptsUs) { ApngFrameDecoder(context) }
            FormatKind.AnimatedWebp -> decodeAnimated(uri, ptsUs) { WebpFrameDecoder(context) }
            else -> decodeSystem(uri, ptsUs)
        }
    }

    private fun decodeSystem(uri: Uri, ptsUs: Long): Bitmap? {
        val decoder = FrameDecoder(context)
        val result = try {
            decoder.open(uri)
            decoder.seekToKeyframe(ptsUs)
            FramePuller.pull(decoder, ptsUs, 1000)?.second
        } catch (e: Exception) {
            logFailure("FrameSourceBitmapLoader", e)
            null
        } finally {
            decoder.close()
        }
        return result ?: retrieverFallback(uri, ptsUs)
    }

    private fun retrieverFallback(uri: Uri, ptsUs: Long): Bitmap? = runCatching {
        android.media.MediaMetadataRetriever().let { retriever ->
            try {
                retriever.setDataSource(context, uri)
                retriever.getFrameAtTime(ptsUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST)?.let { scaleFallback(it) }
            } finally {
                retriever.release()
            }
        }
    }.logged("FrameSourceBitmapLoader").getOrNull()

    private suspend fun decodeFfmpeg(projectId: Long, uri: Uri, ptsUs: Long): Bitmap? = runCatching {
        val dao = AppDatabase.get(context).videoIndexDao()
        val before = dao.atOrBefore(projectId, ptsUs)
        val after = dao.atOrAfter(projectId, ptsUs)
        val target = chooseTarget(before, after, ptsUs)
        val router = DecodeRouter(context)
        try {
            router.open(uri)
            val selected = target ?: return null
            if (selected.nearestKeyframeIndex >= 0) router.seekFfmpegToKeyframe(selected.nearestKeyframeIndex, selected.nearestKeyframePtsUs)
            router.decodeFfmpegFrame(selected.sampleIndex)?.bitmap
        } finally {
            router.close()
        }
    }.logged("FrameSourceBitmapLoader").getOrNull()

    private fun chooseTarget(before: com.frameout.core.db.VideoIndexEntity?, after: com.frameout.core.db.VideoIndexEntity?, ptsUs: Long) = when {
        before == null -> after
        after == null -> before
        kotlin.math.abs(before.ptsUs - ptsUs) <= kotlin.math.abs(after.ptsUs - ptsUs) -> before
        else -> after
    }

    private fun decodeAnimated(uri: Uri, ptsUs: Long, newDecoder: () -> AnimatedFrameSource): Bitmap? = runCatching {
        val decoder = newDecoder()
        try {
            decoder.open(uri)
            decoder.decodeNearest(ptsUs)?.second
        } finally {
            decoder.close()
        }
    }.logged("FrameSourceBitmapLoader").getOrNull()

    private fun scaleFallback(bitmap: Bitmap): Bitmap {
        val max = 4096
        if (bitmap.width <= max && bitmap.height <= max) return bitmap
        val factor = max.toFloat() / maxOf(bitmap.width, bitmap.height)
        val scaled = SmoothScale.scaled(bitmap, (bitmap.width * factor).toInt().coerceAtLeast(1), (bitmap.height * factor).toInt().coerceAtLeast(1), true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }
}
