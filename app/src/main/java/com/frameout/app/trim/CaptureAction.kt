package com.frameout.app.trim

import android.graphics.Bitmap
import android.net.Uri
import com.frameout.core.edit.GainmapOps
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.storage.CaptureSave
import com.frameout.core.storage.CaptureSource
import com.frameout.core.storage.CaptureSize
import com.frameout.core.storage.FrameCaptureSaver
import com.frameout.core.storage.ImageFormat
import com.frameout.core.util.FrameStrings
import com.frameout.engine.player.PlayerController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal sealed interface FrameSaveResult {
    data class Ok(val saved: SavedCapture, val width: Int, val height: Int, val metadataWritten: Boolean) : FrameSaveResult
    data class Failed(val message: String) : FrameSaveResult
}

internal suspend fun saveFrameBitmap(source: Bitmap, saver: FrameCaptureSaver, prefs: AppPrefs, capture: CaptureSource): FrameSaveResult {
    val bitmap = try { withContext(Dispatchers.Default) { CaptureSize.apply(source, prefs.captureSize.value) } } catch (e: CancellationException) {
        source.recycle()
        throw e
    } catch (e: Exception) {
        source.recycle()
        return FrameSaveResult.Failed(FrameStrings.saveFailed())
    }
    try {
        val width = bitmap.width
        val height = bitmap.height
        val format = ImageFormat.resolve(prefs.saveFormat.value, GainmapOps.has(bitmap))
        return when (val result = saver.save(bitmap, format, capture)) {
            is CaptureSave.Saved -> FrameSaveResult.Ok(SavedCapture(result.uri, result.mime), width, height, result.metadataWritten)
            is CaptureSave.Failed -> FrameSaveResult.Failed(saver.failureMessage(result))
        }
    } finally {
        if (bitmap !== source && !bitmap.isRecycled) bitmap.recycle()
        if (!source.isRecycled) source.recycle()
    }
}

internal suspend fun captureFrameToGallery(
    controller: PlayerController,
    saver: FrameCaptureSaver,
    prefs: AppPrefs,
    uri: Uri,
    onCaptured: (Bitmap) -> Unit = {}
): CaptureStatusState {
    prefs.setLastVideo(uri.toString(), controller.positionNow())
    val bitmap = controller.captureFrame() ?: return CaptureStatusState(FrameStrings.captureFailed(), error = true)
    withContext(Dispatchers.Default) { previewOf(bitmap) }?.let(onCaptured)
    val capture = CaptureSource(uri.toString(), controller.positionNow())
    return when (val result = saveFrameBitmap(bitmap, saver, prefs, capture)) {
        is FrameSaveResult.Ok -> { com.frameout.core.rating.Rating.success(); CaptureStatusState(if (result.metadataWritten) FrameStrings.savedDims(result.width, result.height) else FrameStrings.savedNoMetadata(1), result.saved) }
        is FrameSaveResult.Failed -> { com.frameout.core.rating.Rating.failure(); CaptureStatusState(result.message, error = true) }
    }
}

private fun previewOf(source: Bitmap): Bitmap? = runCatching {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return@runCatching null
    val scale = PREVIEW_EDGE.toFloat() / maxOf(source.width, source.height)
    val scaled = if (scale >= 1f) null else Bitmap.createScaledBitmap(
        source,
        (source.width * scale).toInt().coerceAtLeast(1),
        (source.height * scale).toInt().coerceAtLeast(1),
        true
    )
    if (scaled != null && scaled !== source) scaled else source.copy(Bitmap.Config.ARGB_8888, false)
}.getOrNull()

private const val PREVIEW_EDGE = 360

internal suspend fun saveQueuedFrames(
    controller: PlayerController,
    saver: FrameCaptureSaver,
    prefs: AppPrefs,
    uri: Uri,
    positions: List<Long>,
    onProgress: (CaptureStatusState) -> Unit
): CaptureStatusState {
    prefs.setLastVideo(uri.toString(), controller.positionNow())
    var saved = 0
    var failed = 0
    var metadataFailures = 0
    var last: FrameSaveResult.Ok? = null
    var firstError: String? = null
    positions.forEachIndexed { index, position ->
        onProgress(CaptureStatusState(FrameStrings.queueProgress(index + 1, positions.size), busy = true))
        val bitmap = controller.captureAt(position)
        if (bitmap == null) {
            failed++
            if (firstError == null) firstError = FrameStrings.captureFailed()
            return@forEachIndexed
        }
        when (val result = saveFrameBitmap(bitmap, saver, prefs, CaptureSource(uri.toString(), position, index + 1))) {
            is FrameSaveResult.Ok -> {
                saved++
                if (!result.metadataWritten) metadataFailures++
                last = result
            }
            is FrameSaveResult.Failed -> {
                failed++
                if (firstError == null) firstError = result.message
            }
        }
    }
    return when {
        saved == 0 -> CaptureStatusState(firstError ?: FrameStrings.nothingSaved(), error = true)
        failed > 0 && metadataFailures > 0 -> CaptureStatusState(FrameStrings.queueSavedPartialMetadata(saved, failed, metadataFailures), last?.saved, error = true)
        failed > 0 -> CaptureStatusState(FrameStrings.queueSavedPartial(saved, failed), last?.saved, error = true)
        metadataFailures > 0 -> CaptureStatusState(FrameStrings.savedNoMetadata(metadataFailures), last?.saved)
        else -> CaptureStatusState(FrameStrings.savedN(saved), last?.saved)
    }
}
