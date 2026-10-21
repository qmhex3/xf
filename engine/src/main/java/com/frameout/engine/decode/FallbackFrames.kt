package com.frameout.engine.decode

import com.frameout.core.util.logged
import com.frameout.core.util.logFailure
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

internal class FallbackFrames(private val context: Context) {
    private var retriever: MediaMetadataRetriever? = null
    private var uri: Uri? = null

    fun bind(source: Uri) {
        uri = source
    }

    fun frameAt(ptsUs: Long): Bitmap? = runCatching {
        if (ptsUs < 0L) return null
        val active = retriever ?: MediaMetadataRetriever().also {
            it.setDataSource(context, uri ?: return null)
            retriever = it
        }
        fullFrame(active, ptsUs)?.let { FallbackColor.normalize(it) }
    }.logged("FallbackFrames").getOrNull()

    private fun fullFrame(active: MediaMetadataRetriever, ptsUs: Long): Bitmap? = try {
        active.getFrameAtTime(ptsUs, MediaMetadataRetriever.OPTION_CLOSEST)
    } catch (e: OutOfMemoryError) {
        logFailure("FallbackFrames", e)
        reduced(active, ptsUs)
    }

    private fun reduced(active: MediaMetadataRetriever, ptsUs: Long): Bitmap? {
        if (android.os.Build.VERSION.SDK_INT < 27) return null
        DecodeQuality.markReduced("full size fallback frame did not fit in memory at $ptsUs, scaled to $MAX_SIDE")
        return active.getScaledFrameAtTime(ptsUs, MediaMetadataRetriever.OPTION_CLOSEST, MAX_SIDE, MAX_SIDE)
    }

    fun close() {
        try { retriever?.release() } catch (_: Exception) {}
        retriever = null
        uri = null
    }

    private companion object {
        const val MAX_SIDE = 2048
    }
}
