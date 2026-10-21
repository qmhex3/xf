package com.frameout.engine.player

import com.frameout.core.util.logged
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.frameout.codecpack.FormatKind
import com.frameout.codecpack.FormatProbe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FrameGrabber(private val context: Context, private val uri: Uri) {
    private val lock = Any()
    private var retriever: MediaMetadataRetriever? = null
    private var systemPath: Boolean? = null
    private var closed = false
    private val exact = ExactFrameDecoder(context, uri)

    suspend fun grab(positionMs: Long): Bitmap? {
        val app = context.applicationContext
        if (systemPath == null) {
            systemPath = runCatching { FormatProbe.probe(app, uri).kind != FormatKind.NeedsFfmpeg }.getOrDefault(true)
        }
        if (systemPath != true) return FrameStepper.capture(app, uri, positionMs)
        return withContext(Dispatchers.IO) {
            val ptsUs = ExactPts.displayedAt(app, uri, positionMs) ?: (positionMs.coerceAtLeast(0L) * 1000L)
            exact.decode(ptsUs) ?: grabSystem(app, ptsUs)
        } ?: FrameStepper.capture(app, uri, positionMs)
    }

    fun close() {
        exact.close()
        synchronized(lock) {
            closed = true
            retriever?.let { runCatching { it.release() } }
            retriever = null
        }
    }

    private fun grabSystem(app: Context, ptsUs: Long): Bitmap? = synchronized(lock) {
        if (closed) return null
        val active = retriever ?: MediaMetadataRetriever().also {
            val opened = runCatching { it.setDataSource(app, uri) }.isSuccess
            if (!opened) {
                runCatching { it.release() }
                return null
            }
            retriever = it
        }
        val frame = runCatching { active.getFrameAtTime(ptsUs, MediaMetadataRetriever.OPTION_CLOSEST) }.logged("FrameGrabber").getOrNull()
        if (frame == null) {
            runCatching { active.release() }
            retriever = null
        }
        frame
    }
}
