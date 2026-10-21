package com.frameout.engine.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import com.frameout.codecpack.FormatKind
import com.frameout.codecpack.FormatProbe
import com.frameout.engine.decode.FfmpegFrameSource
import com.frameout.engine.decode.AnimatedFrameSource
import com.frameout.engine.decode.StillImageSource
import com.frameout.engine.decode.WebpFrameDecoder
import com.frameout.engine.decode.AnimatedSources
import com.frameout.engine.decode.ImageHead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class AnimatedPlayback private constructor(
    private val source: AnimatedFrameSource,
    private val scope: CoroutineScope,
    private val onFrame: (Bitmap) -> Unit,
    val stillOnly: Boolean = false
) {
    private val lock = Mutex()
    private val frameCount = source.frameCount().coerceAtLeast(1)
    private val durationUs = source.durationUs().coerceAtLeast(1_000L)
    private var shown = -1
    private var loopJob: Job? = null
    private var seekJob: Job? = null

    @Volatile var positionUs: Long = 0L
        private set
    @Volatile var playing: Boolean = false
        private set
    @Volatile var speed: Float = 1f
        private set

    val durationMs: Long = (durationUs / 1000L).coerceAtLeast(1L)
    val aspect: Float = if (source.videoHeight() > 0) source.videoWidth().toFloat() / source.videoHeight() else 0f

    private fun indexAt(us: Long): Int {
        var low = 0
        var high = frameCount - 1
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (source.ptsUs(mid) <= us) low = mid else high = mid - 1
        }
        return low
    }

    private suspend fun decode(index: Int): Bitmap? =
        lock.withLock { withContext(Dispatchers.IO) { source.decodeFrame(index)?.second } }

    private suspend fun show(index: Int) {
        if (index == shown) return
        val bitmap = decode(index) ?: return
        shown = index
        onFrame(bitmap)
    }

    suspend fun showFirst() = show(0)

    fun play() {
        if (playing || stillOnly) return
        playing = true
        loopJob?.cancel()
        loopJob = scope.launch {
            var anchorWall = SystemClock.elapsedRealtime()
            var anchorUs = positionUs
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                var pos = anchorUs + ((now - anchorWall) * 1000L * speed).toLong()
                if (pos >= durationUs) {
                    pos %= durationUs
                    anchorUs = pos
                    anchorWall = now
                }
                positionUs = pos
                show(indexAt(pos))
                val next = if (shown + 1 < frameCount) source.ptsUs(shown + 1) else durationUs
                val wait = (((next - pos) / 1000f) / speed).toLong().coerceIn(1L, 100L)
                delay(wait)
            }
        }
    }

    fun pause() {
        playing = false
        loopJob?.cancel()
        loopJob = null
    }

    fun setSpeed(value: Float) {
        val wasPlaying = playing
        pause()
        speed = value
        if (wasPlaying) play()
    }

    fun seekTo(ms: Long) {
        positionUs = (ms * 1000L).coerceIn(0L, durationUs - 1L)
        if (playing) {
            pause()
            play()
        } else {
            seekJob?.cancel()
            val target = indexAt(positionUs)
            seekJob = scope.launch { show(target) }
        }
    }

    suspend fun step(delta: Int): Long {
        pause()
        val target = (indexAt(positionUs) + delta).coerceIn(0, frameCount - 1)
        positionUs = source.ptsUs(target)
        show(target)
        return positionUs / 1000L
    }

    suspend fun captureAt(ms: Long): Bitmap? = decode(indexAt((ms * 1000L).coerceIn(0L, durationUs - 1L)))

    fun close() {
        pause()
        seekJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch { lock.withLock { runCatching { source.close() } } }
    }

    companion object {
        fun looksAnimated(context: Context, uri: Uri): Boolean {
            val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty().lowercase()
            if (mime in ANIMATED_MIMES) return true
            val name = runCatching {
                if (uri.scheme == "content") {
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) c.getString(0) else null
                    }
                } else null
            }.getOrNull() ?: uri.lastPathSegment.orEmpty()
            return name.substringAfterLast('.', "").lowercase() in ANIMATED_EXTENSIONS
        }

        fun isExoHopeless(context: Context, uri: Uri): Boolean = ImageHead.read(context, uri).let { it == ImageHead.Kind.AnimatedWebp || it == ImageHead.Kind.Avif }

        suspend fun open(context: Context, uri: Uri, scope: CoroutineScope, onFrame: (Bitmap) -> Unit): AnimatedPlayback? =
            withContext(Dispatchers.IO) {
                val app = context.applicationContext
                val decoder = chooseSource(app, uri) ?: return@withContext null
                try {
                    decoder.open(uri)
                    if (decoder.frameCount() <= 0) error("no frames")
                    AnimatedPlayback(decoder, scope, onFrame, decoder is StillImageSource).also { it.showFirst() }
                } catch (e: Exception) {
                    runCatching { decoder.close() }
                    null
                }
            }

        suspend fun openVideo(context: Context, uri: Uri, scope: CoroutineScope, force: Boolean, onFrame: (Bitmap) -> Unit): AnimatedPlayback? =
            withContext(Dispatchers.IO) {
                val app = context.applicationContext
                val needed = force || runCatching { FormatProbe.probe(app, uri).kind == FormatKind.NeedsFfmpeg }.getOrDefault(false)
                if (!needed) return@withContext null
                val decoder = FfmpegFrameSource(app)
                try {
                    decoder.open(uri)
                    AnimatedPlayback(decoder, scope, onFrame).also { it.showFirst() }
                } catch (e: Exception) {
                    runCatching { decoder.close() }
                    null
                }
            }

        private fun chooseSource(app: Context, uri: Uri): AnimatedFrameSource? {
            when (ImageHead.read(app, uri)) {
                ImageHead.Kind.AnimatedWebp -> return WebpFrameDecoder(app)
                ImageHead.Kind.Avif -> return StillImageSource(app)
                else -> Unit
            }
            val kind = runCatching { FormatProbe.probe(app, uri).kind }.getOrNull() ?: return null
            return AnimatedSources.create(app, kind)
        }

        private val ANIMATED_MIMES = setOf("image/gif", "image/apng", "image/png", "image/webp", "image/avif")
        private val ANIMATED_EXTENSIONS = setOf("gif", "apng", "png", "webp", "avif")
    }

}
