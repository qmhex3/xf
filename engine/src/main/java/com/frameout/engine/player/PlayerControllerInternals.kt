package com.frameout.engine.player

import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun PlayerController.adopt(clip: AnimatedPlayback) {
    animated = clip
    dropStill()
    val notice = if (clip.stillOnly) PreviewNotice.StillOnly else PreviewNotice.None
    _state.value = _state.value.copy(animated = true, ready = true, isPlaying = false, durationMs = clip.durationMs, videoAspect = clip.aspect, notice = notice)
}

internal fun PlayerController.fallbackToFfmpeg() {
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

internal fun PlayerController.startExo(uri: Uri) {
    grabber = FrameGrabber(context.applicationContext, uri)
    player.setMediaItem(MediaItem.fromUri(uri))
    player.prepare()
    player.playWhenReady = false
}
internal fun PlayerController.outsideWindow(positionMs: Long): Boolean =
    windowActive && WindowRules.isOutside(windowStartMs, windowEndMs, positionMs)

internal fun PlayerController.enforceWindow() {
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
internal fun PlayerController.settleInWindow() {
    if (!windowActive || scrubbing) return
    val now = animated?.let { it.positionUs / 1000L } ?: player.currentPosition
    WindowRules.settleAfterPause(windowStartMs, windowEndMs, now)?.let { seekTo(it) }
}
internal fun PlayerController.applySpeed(speed: Float) {
    if (released) return
    val clip = animated
    if (clip != null) clip.setSpeed(speed) else player.setPlaybackSpeed(speed)
}
internal fun PlayerController.noteBuffering(raw: Boolean) {
    if (!raw) bufferingSinceMs = 0L
    else if (bufferingSinceMs == 0L) bufferingSinceMs = SystemClock.elapsedRealtime()
}

internal fun PlayerController.spinnerVisible(): Boolean =
    !scrubbing && bufferingSinceMs != 0L && SystemClock.elapsedRealtime() - bufferingSinceMs >= SPINNER_DELAY_MS
internal fun PlayerController.refresh() {
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
    syncStill()
}
internal fun PlayerController.dropStill() {
    stillJob?.cancel()
    stillJob = null
    pendingStillMs = NO_POSITION
    stillMs = NO_POSITION
    if (_still.value != null) _still.value = null
}

internal fun PlayerController.syncStill() {
    val s = _state.value
    val wanted = HdrPreviewPolicy.wantsStill(hdrStillEnabled, sourceHdr, windowHdr, animated != null, s.isPlaying, scrubbing, s.ready)
    if (!wanted) {
        dropStill()
        return
    }
    val at = _position.value
    if (at == stillMs || at == pendingStillMs) return
    requestStill(at)
}

internal fun PlayerController.requestStill(at: Long) {
    stillJob?.cancel()
    if (_still.value != null) _still.value = null
    stillMs = NO_POSITION
    pendingStillMs = at
    stillJob = scope.launch {
        delay(HdrPreviewPolicy.DEBOUNCE_MS)
        val bitmap = captureAt(at)?.let { fitForPreview(it) }
        val s = _state.value
        val current = !released && bitmap != null && pendingStillMs == at && _position.value == at && !s.isPlaying && !scrubbing
        if (current) {
            _still.value = bitmap
            stillMs = at
        }
    }
}

internal fun PlayerController.fitForPreview(bitmap: Bitmap): Bitmap {
    val (w, h) = HdrPreviewPolicy.scaledSize(bitmap.width, bitmap.height)
    return if (w == bitmap.width && h == bitmap.height) bitmap else Bitmap.createScaledBitmap(bitmap, w, h, true)
}
