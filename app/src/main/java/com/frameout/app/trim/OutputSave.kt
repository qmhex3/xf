package com.frameout.app.trim

import android.content.Context
import android.net.Uri
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.logFailure
import com.frameout.engine.timeline.TrimRange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class SaveResult(val message: String, val error: Boolean = false) {
    companion object {
        fun ok(message: String) = SaveResult(message)
        fun fail(message: String) = SaveResult(message, error = true)
    }
}

internal class OutputSavers(context: Context) {
    private val clip = ClipSaver(context)
    private val motion = MotionClipSaver(context)

    suspend fun save(output: TrimOutput, uri: Uri, range: TrimRange, positionMs: Long, durationMs: Long, onProgress: (Int) -> Unit): SaveResult = when (output) {
        TrimOutput.Frames -> SaveResult.ok("")
        TrimOutput.Video -> clip.saveMp4(uri, range, durationMs, onProgress)
        TrimOutput.Webp -> clip.saveWebp(uri, range, durationMs, onProgress)
        TrimOutput.Gif -> motion.saveGif(uri, range, onProgress)
        TrimOutput.Live -> motion.saveLive(uri, range, positionMs, durationMs, onProgress)
    }
}

internal fun startOutputSave(ui: TrimUiState, scope: CoroutineScope, savers: OutputSavers, uri: Uri, range: TrimRange, positionMs: Long, durationMs: Long) {
    if (ui.busy) return
    ui.busy = true
    ui.status = CaptureStatusState(FrameStrings.saving(), busy = true)
    val output = ui.output
    ui.saveJob = scope.launch {
        val result = try {
            savers.save(output, uri, range, positionMs, durationMs, ui::progress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFailure("TrimSave", e)
            SaveResult.fail(FrameStrings.saveFailed())
        } finally {
            ui.saveEnded()
        }
        ui.status = CaptureStatusState(result.message, error = result.error, kind = output.saveKind)
    }
}
