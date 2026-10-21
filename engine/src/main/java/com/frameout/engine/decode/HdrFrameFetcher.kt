package com.frameout.engine.decode

import com.frameout.core.util.logFailure
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.frameout.core.edit.GainmapOps
import com.frameout.core.hdr.HdrFrameSource

class HdrFrameFetcher(private val context: Context) : HdrFrameSource {
    private var lastUri: Uri? = null
    private var lastHdr = false
    private var decoder: FrameDecoder? = null
    private var openUri: Uri? = null
    private val scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "hdr-fetch-idle").apply { isDaemon = true } }
    private var pending: java.util.concurrent.ScheduledFuture<*>? = null

    override fun fetch(source: Uri, ptsUs: Long): Bitmap? = fetchWith(source, ptsUs, false)

    @Synchronized
    override fun isHdrSource(source: Uri): Boolean = isHdr(source)

    override fun fetchExact(source: Uri, ptsUs: Long): Bitmap? = fetchWith(source, ptsUs, true)?.takeIf { com.frameout.core.hdr.HdrSignalRegistry.has(it) || discard(it) }

    @Synchronized
    private fun fetchWith(source: Uri, ptsUs: Long, exact: Boolean): Bitmap? {
        if (!isHdr(source)) return null
        return try {
            val active = acquire(source)
            active.captureSignal = exact
            pending?.cancel(false)
            if (!canContinue(active, ptsUs)) active.seekToKeyframe(ptsUs)
            val result = nearest(active, ptsUs)?.takeIf { GainmapOps.has(it) || discard(it) }
            active.captureSignal = false
            pending = scheduler.schedule(Runnable { release() }, IDLE_RELEASE_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            result
        } catch (e: Exception) {
            logFailure("HdrFrameFetcher", e)
            release()
            null
        } catch (e: OutOfMemoryError) {
            release()
            null
        }
    }

    private fun canContinue(active: FrameDecoder, target: Long): Boolean {
        val last = active.lastOutputPtsUs
        return !active.drained && last != Long.MIN_VALUE && target > last && target - last <= CONTINUE_WINDOW_US
    }

    @Synchronized
    fun release() {
        pending?.cancel(false)
        pending = null
        decoder?.close()
        decoder = null
        openUri = null
    }

    private fun acquire(source: Uri): FrameDecoder {
        val current = decoder
        if (current != null && openUri == source) return current
        release()
        val fresh = FrameDecoder(context)
        try {
            fresh.open(source)
        } catch (e: Exception) {
            fresh.close()
            throw e
        }
        decoder = fresh
        openUri = source
        return fresh
    }

    private fun isHdr(source: Uri): Boolean {
        if (source != lastUri) {
            lastHdr = HdrDetector.isHdr(context, source)
            lastUri = source
        }
        return lastHdr
    }

    private fun discard(bitmap: Bitmap): Boolean {
        bitmap.recycle()
        return false
    }

    private fun nearest(decoder: FrameDecoder, target: Long): Bitmap? = FramePuller.pull(decoder, target, MAX_FRAMES, strict = true)?.second

    private companion object {
        const val MAX_FRAMES = 100_000
        const val IDLE_RELEASE_MS = 10_000L
        const val CONTINUE_WINDOW_US = 4_000_000L
    }
}
