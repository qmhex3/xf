package com.frameout.app.viewer

import android.app.Application
import android.content.Context
import android.content.Intent
import com.frameout.app.search.ImageSearchLauncher
import com.frameout.core.db.FrameEntity
import com.frameout.core.search.SearchEngine
import com.frameout.core.storage.FrameExporter
import com.frameout.core.storage.SaveFailure
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ViewerExport(
    private val scope: CoroutineScope,
    private val exporter: FrameExporter,
    private val state: MutableStateFlow<ViewerState>,
    private val app: Application
) {
    private var running = false

    fun save(frame: FrameEntity) = runSaving {
        val saved = exporter.saveToGallery(listOf(frame)) > 0
        val lost = exporter.lastMetadataFailures
        if (saved) com.frameout.core.rating.Rating.success() else com.frameout.core.rating.Rating.failure()
        val message = if (saved && lost > 0) FrameStrings.savedNoMetadata(lost) else if (saved) FrameStrings.savedOk() else SaveFailure.message(app, FrameStrings.saveFailed())
        message to saved
    }

    fun replaceSaved(savedId: Long, frame: FrameEntity) = runSaving {
        val ok = exporter.resaveFromFrame(savedId, frame)
        (if (ok) FrameStrings.savedAgain() else SaveFailure.message(app, FrameStrings.resaveFailed())) to ok
    }

    private fun runSaving(block: suspend () -> Pair<String, Boolean>) {
        if (running) return
        running = true
        val busyText = FrameStrings.saving()
        state.update { it.copy(message = busyText, busyMessage = busyText, messageSaved = null) }
        scope.launch {
            var result = SaveFailure.message(app, FrameStrings.saveFailed()) to false
            try {
                result = block()
            } catch (e: CancellationException) {
                state.update { if (it.messageBusy) it.copy(message = null, busyMessage = null) else it }
                throw e
            } catch (e: Exception) {
                logFailure("ViewerSave", e)
            } finally {
                running = false
            }
            val (text, ok) = result
            state.update { it.copy(message = text, busyMessage = null, messageSaved = if (ok) com.frameout.designsystem.icons.SaveKind.Image else null) }
        }
    }

    fun share(frame: FrameEntity, onReady: (Intent) -> Unit) {
        scope.launch { exporter.shareIntent(listOf(frame))?.let(onReady) }
    }

    fun search(context: Context, engine: SearchEngine, frame: FrameEntity) {
        scope.launch {
            val busyText = com.frameout.core.util.SearchStrings.opening()
            state.update { it.copy(message = busyText, busyMessage = busyText, messageSaved = null) }
            val message = ImageSearchLauncher.run(context, exporter, engine, frame)
            if (message != null) state.update { it.copy(message = message, busyMessage = null) }
            else {
                kotlinx.coroutines.delay(com.frameout.app.search.SEARCH_HANDOFF_MS)
                state.update { if (it.busyMessage == busyText) it.copy(message = null, busyMessage = null) else it }
            }
        }
    }
}
