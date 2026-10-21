package com.frameout.app.frames

import android.app.Application
import com.frameout.core.db.FrameEntity
import com.frameout.core.storage.FrameExporter
import com.frameout.core.storage.SaveFailure
import com.frameout.core.util.DiagLog
import com.frameout.core.util.FrameStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PROGRESS_STEP = 5
private const val UNDO_MIN = 20

internal class FramesGallerySave(
    private val scope: CoroutineScope,
    private val app: Application,
    private val exporter: FrameExporter,
    private val state: MutableStateFlow<FramesState>,
    private val selectedFrames: suspend () -> List<FrameEntity>
) {
    private var job: Job? = null

    fun cancel() {
        job?.cancel()
    }

    fun confirm() {
        if (state.value.gallerySave != null) {
            state.update { it.copy(confirmGallerySave = 0) }
            return
        }
        state.update { it.copy(confirmGallerySave = 0, gallerySave = GallerySaveProgress(0, it.selected.size), message = null, undoGalleryIds = emptyList()) }
        job = scope.launch { run() }
    }

    private suspend fun run() {
        var began = false
        try {
            val frames = selectedFrames()
            state.update { it.copy(gallerySave = GallerySaveProgress(0, frames.size)) }
            began = true
            exporter.saveToGallery(frames) { done -> report(done, frames.size) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DiagLog.append("gallery", "save failed: ${e.message}")
        } finally {
            val cancelled = !currentCoroutineContext().isActive
            withContext(NonCancellable + Dispatchers.IO) { finish(began, cancelled) }
        }
    }

    private fun report(done: Int, total: Int) {
        if (done % PROGRESS_STEP == 0 || done == total) state.update { it.copy(gallerySave = GallerySaveProgress(done, total)) }
    }

    private fun finish(began: Boolean, cancelled: Boolean) {
        val ids = if (began) exporter.lastSavedIds else emptyList()
        val lost = if (began) exporter.lastMetadataFailures else 0
        val message = when {
            ids.isNotEmpty() && lost > 0 -> FrameStrings.savedNoMetadata(lost)
            ids.isNotEmpty() -> FrameStrings.savedN(ids.size)
            cancelled -> null
            else -> SaveFailure.message(app, FrameStrings.nothingSaved())
        }
        val undo = if (ids.size >= UNDO_MIN) ids else emptyList()
        state.update { it.copy(gallerySave = null, message = message, messageSaved = if (ids.isNotEmpty()) com.frameout.designsystem.icons.SaveKind.Image else null, undoGalleryIds = undo) }
    }
}
