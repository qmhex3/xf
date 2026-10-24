package com.frameout.engine.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Tracks
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.graphics.Bitmap
import android.os.SystemClock

enum class PreviewNotice { None, StillOnly, Unavailable }

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val ready: Boolean = false,
    val speed: Float = PlaybackSpeeds.NORMAL,
    val videoAspect: Float = 0f,
    val animated: Boolean = false,
    val notice: PreviewNotice = PreviewNotice.None,
    val hasAudio: Boolean = false,
    val muted: Boolean = false,
    val buffering: Boolean = false
)

class PlayerController(private val context: Context) {
    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext, DefaultRenderersFactory(context.applicationContext).setEnableDecoderFallback(true))
        .setLoadControl(smoothLoadControl())
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        .setHandleAudioBecomingNoisy(true)
        .build()
    private val _state = MutableStateFlow(PlayerUiState())
    private val _position = MutableStateFlow(0L)
    private var mediaUri: Uri? = null
    private var grabber: FrameGrabber? = null
    private val steps = StepQueue(::performStep)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _frame = MutableStateFlow<Bitmap?>(null)
    private var animated: AnimatedPlayback? = null
    private var openJob: Job? = null
    private var released = false
    private var fallbackTried = false
    @Volatile private var windowStartMs = -1L
    @Volatile private var windowEndMs = -1L
    @Volatile private var scrubbing = false
    @Volatile private var scrubTargetMs = -1L
    @Volatile private var bufferingSinceMs = 0L
    val frame: StateFlow<Bitmap?> = _frame.asStateFlow()
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()
    val position: StateFlow<Long> = _position.asStateFlow()
    val playing: Boolean get() = animated?.playing ?: player.isPlaying

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width <= 0 || videoSize.height <= 0) return
            val swapped = videoSize.unappliedRotationDegrees % 180 != 0
            val w = videoSize.width * videoSize.pixelWidthHeightRatio
            val h = videoSize.height.toFloat()
            _state.value = _state.value.copy(videoAspect = if (swapped) h / w else w / h)
        }

        override fun onTracksChanged(tracks: Tracks) {
            _state.value = _state.value.copy(hasAudio = tracks.containsType(C.TRACK_TYPE_AUDIO))
            if (tracks.containsType(C.TRACK_TYPE_VIDEO) && !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO)) fallbackToFfmpeg()
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(ready = false, isPlaying = false, notice = PreviewNotice.Unavailable)
            fallbackToFfmpeg()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            noteBuffering(playbackState == Player.STATE_BUFFERING)
            _state.value = _state.value.copy(buffering = spinnerVisible())
            if (playbackState == Player.STATE_READY) {
                _state.value = _state.value.copy(ready = true, durationMs = player.duration.coerceAtLeast(0))
            }
        }
    }

    init {
        player.addListener(listener)
        player.repeatMode = Player.REPEAT_MODE_OFF
    }

    fun attach(): ExoPlayer = player
    fun setUri(uri: Uri) {
        mediaUri = uri
        fallbackTried = false
        openJob?.cancel()
        animated?.close()
        animated = null
        _frame.value = null
        _state.value = _state.value.copy(animated = false, notice = PreviewNotice.None, hasAudio = false)
        grabber?.close()
        grabber = null
        player.stop()
        val app = context.applicationContext
        openJob = scope.launch {
            val candidate = AnimatedPlayback.looksAnimated(app, uri)
            val still = if (candidate) AnimatedPlayback.open(app, uri, scope) { _frame.value = it } else null
            val clip = still ?: AnimatedPlayback.openVideo(app, uri, scope, false) { _frame.value = it }
            val hopeless = clip == null && ((candidate && AnimatedPlayback.isExoHopeless(app, uri)) || PreviewSupport.exoCannotPlay(app, uri))
            withContext(Dispatchers.Main) {
                if (released || mediaUri != uri) {
                    clip?.close()
                } else if (clip != null) {
                    adopt(clip)
                } else {
                    startExo(uri)
                }
            }
        }
    }

    private fun adopt(clip: AnimatedPlayback) {
        animated = clip
        val notice = if (clip.stillOnly) PreviewNotice.StillOnly else PreviewNotice.None
        _state.value = _state.value.copy(animated = true, ready = true, isPlaying = false, durationMs = clip.durationMs, videoAspect = clip.aspect, notice = notice)
    }

    private fun fallbackToFfmpeg() {
        val uri = mediaUri ?: return
        if (animated != null || fallbackTried) return
        fallbackTried = true
        scope.launch {
            val clip = AnimatedPlayback.openVideo(context.applicationContext, uri, scope, true) { _frame.value = it }
            withContext(Dispatchers.Main) {
                if (released || mediaUri != uri || animated != null || clip == null) clip?.close() else { player.stop(); adopt(clip) }
            }
        }
    }

    private fun startExo(uri: Uri) {
        grabber = FrameGrabber(context.applicationContext, uri)
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = false
    }

    fun setWindow(startMs: Long, endMs: Long) {
        if (endMs - startMs < MIN_WINDOW_MS) {
            clearWindow()
            return
        }
        windowStartMs = startMs.coerceAtLeast(0L)
        windowEndMs = endMs
    }

    fun clearWindow() {
        windowStartMs = -1L
        windowEndMs = -1L
    }

    private val windowActive: Boolean get() = windowStartMs >= 0L && windowEndMs > windowStartMs

    private fun outsideWindow(positionMs: Long): Boolean =
        windowActive && WindowRules.isOutside(windowStartMs, windowEndMs, positionMs)

    private fun enforceWindow() {
        if (!windowActive || scrubbing) return
        if (animated == null && player.playbackState == Player.STATE_ENDED && player.playWhenReady) {
            player.seekTo(windowStartMs)
            player.play()
            refresh()
            return
        }
        if (!playing) return
        val now = _position.value
        if (outsideWindow(now)) seekTo(windowStartMs)
    }

    fun play() {
        animated?.let {
            if (outsideWindow(_position.value)) seekTo(windowStartMs)
            it.play()
            return
        }
        val duration = player.duration
        val atEnd = player.playbackState == Player.STATE_ENDED || (duration > 0 && player.currentPosition >= duration - END_TOLERANCE_MS)
        if (windowActive && outsideWindow(player.currentPosition)) player.seekTo(windowStartMs)
        else if (atEnd) player.seekTo(0L)
        player.play()
    }

    fun pause() {
        val wasPlaying = playing
        val clip = animated
        if (clip != null) clip.pause() else player.pause()
        if (wasPlaying) settleInWindow()
    }

    private fun settleInWindow() {
        if (!windowActive || scrubbing) return
        val now = animated?.let { it.positionUs / 1000L } ?: player.currentPosition
        WindowRules.settleAfterPause(windowStartMs, windowEndMs, now)?.let(::seekTo)
    }

    fun repairPlayhead() {
        if (!windowActive || scrubbing || playing) return
        WindowRules.repairAfterRangeChange(windowStartMs, windowEndMs, _position.value)?.let(::seekTo)
    }

    fun setMuted(muted: Boolean) {
        player.volume = if (muted) 0f else 1f
        _state.value = _state.value.copy(muted = muted)
    }

    fun cycleSpeed() {
        val speed = PlaybackSpeeds.next(_state.value.speed)
        applySpeed(speed)
        _state.value = _state.value.copy(speed = speed)
    }

    fun holdSpeed(speed: Float) = applySpeed(speed)

    fun releaseSpeed() = applySpeed(_state.value.speed)

    private fun applySpeed(speed: Float) {
        if (released) return
        val clip = animated
        if (clip != null) clip.setSpeed(speed) else player.setPlaybackSpeed(speed)
    }

    fun toggle() {
        if (playing) pause() else play()
    }

    suspend fun stepFrame(forward: Boolean, count: Int = 1) = steps.push(if (forward) count else -count)

    private suspend fun performStep(delta: Int) {
        val uri = mediaUri ?: return
        animated?.let { clip ->
            _position.value = clip.step(delta)
            return
        }
        player.pause()
        val from = player.currentPosition
        val target = withContext(Dispatchers.IO) {
            FrameStepper.step(context.applicationContext, uri, from, delta > 0, kotlin.math.abs(delta))
        } ?: return
        seekTo(target)
    }

    suspend fun captureFrame(): Bitmap? = captureAt(player.currentPosition)

    suspend fun captureAt(positionMs: Long): Bitmap? {
        val uri = mediaUri ?: return null
        animated?.let { return it.captureAt(positionMs) }
        return grabber?.grab(positionMs) ?: FrameStepper.capture(context.applicationContext, uri, positionMs)
    }

    fun seekTo(ms: Long) {
        animated?.let {
            it.seekTo(ms)
            _position.value = ms.coerceIn(0L, it.durationMs)
            return
        }
        val d = player.duration.coerceAtLeast(0)
        val target = ms.coerceIn(0, if (d > 0) d else ms)
        if (scrubbing) scrubTargetMs = target
        player.seekTo(target)
        refresh()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun scrub(active: Boolean) {
        scrubbing = active
        player.setScrubbingModeEnabled(active)
        player.setSeekParameters(if (active) SeekParameters.CLOSEST_SYNC else SeekParameters.EXACT)
        if (!active) {
            val target = scrubTargetMs
            scrubTargetMs = -1L
            if (target >= 0L && animated == null && !released) player.seekTo(target)
        }
    }

    private fun noteBuffering(raw: Boolean) {
        if (!raw) bufferingSinceMs = 0L
        else if (bufferingSinceMs == 0L) bufferingSinceMs = SystemClock.elapsedRealtime()
    }

    private fun spinnerVisible(): Boolean =
        !scrubbing && bufferingSinceMs != 0L && SystemClock.elapsedRealtime() - bufferingSinceMs >= SPINNER_DELAY_MS

    fun positionNow(): Long = _position.value
    fun poll() {
        refresh()
        enforceWindow()
    }

    private fun refresh() {
        animated?.let { clip ->
            _position.value = (clip.positionUs / 1000L).coerceAtLeast(0L)
            _state.value = _state.value.copy(isPlaying = clip.playing, durationMs = clip.durationMs, ready = true)
            return
        }
        _position.value = player.currentPosition.coerceAtLeast(0)
        val st = player.playbackState
        noteBuffering(st == Player.STATE_BUFFERING)
        _state.value = _state.value.copy(
            isPlaying = player.isPlaying,
            durationMs = player.duration.coerceAtLeast(0),
            buffering = spinnerVisible(),
            ready = st == Player.STATE_READY || st == Player.STATE_BUFFERING || st == Player.STATE_ENDED
        )
    }

    fun release() {
        released = true
        openJob?.cancel()
        animated?.close()
        animated = null
        scope.cancel()
        player.removeListener(listener)
        mediaUri = null
        grabber?.close()
        grabber = null
        player.release()
    }
}

private const val END_TOLERANCE_MS = WindowRules.END_TOLERANCE_MS
private const val MIN_WINDOW_MS = 100L

private const val SPINNER_DELAY_MS = 900L

private fun smoothLoadControl(): DefaultLoadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(15_000, 40_000, 250, 600)
    .setBackBuffer(15_000, true)
    .setPrioritizeTimeOverSizeThresholds(true)
    .build()
