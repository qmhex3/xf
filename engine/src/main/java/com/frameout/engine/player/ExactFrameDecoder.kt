package com.frameout.engine.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.frameout.core.util.logged
import com.frameout.engine.decode.FrameDecoder
import com.frameout.engine.decode.FramePuller

internal class ExactFrameDecoder(private val context: Context, private val uri: Uri) {
    private val lock = Any()
    private var decoder: FrameDecoder? = null
    private var closed = false
    private var idle: java.util.concurrent.ScheduledFuture<*>? = null
    private val scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "exact-frame-idle").apply { isDaemon = true } }

    fun decode(ptsUs: Long): Bitmap? = synchronized(lock) {
        if (closed) return null
        idle?.cancel(false)
        val active = acquire() ?: return null
        val result = runCatching {
            if (!canContinue(active, ptsUs)) active.seekToKeyframe(ptsUs)
            FramePuller.pull(active, ptsUs, MAX_FRAMES, strict = true)?.second
        }.logged("ExactFrameDecoder").getOrNull()
        if (result == null) discard() else idle = scheduler.schedule(Runnable { release() }, IDLE_RELEASE_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
        result
    }

    fun close() {
        synchronized(lock) {
            closed = true
            idle?.cancel(false)
            discard()
        }
        scheduler.shutdown()
    }

    private fun release() {
        synchronized(lock) { discard() }
    }

    private fun acquire(): FrameDecoder? {
        decoder?.let { return it }
        val fresh = FrameDecoder(context)
        val opened = runCatching { fresh.open(uri) }.logged("ExactFrameDecoder").isSuccess
        if (!opened) {
            fresh.close()
            return null
        }
        decoder = fresh
        return fresh
    }

    private fun discard() {
        decoder?.let { runCatching { it.close() } }
        decoder = null
    }

    private fun canContinue(active: FrameDecoder, target: Long): Boolean {
        val last = active.lastOutputPtsUs
        return !active.drained && last != Long.MIN_VALUE && target > last && target - last <= CONTINUE_WINDOW_US
    }

    private companion object {
        const val IDLE_RELEASE_MS = 8_000L
        const val MAX_FRAMES = 100_000
        const val CONTINUE_WINDOW_US = 2_000_000L
    }
}
