package com.frameout.app.library

import androidx.lifecycle.viewModelScope
import androidx.paging.map
import com.frameout.app.deletionMessage
import com.frameout.core.db.deleteManyChunked
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.util.SavedStrings
import com.frameout.designsystem.icons.FrameIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun LibraryViewModel.deleteSelectedSaved() = viewModelScope.launch {
    val ids = _state.value.selectedSaved.toList()
    if (ids.isEmpty()) return@launch
    undoJob?.cancel()
    _state.update { it.copy(undoSavedCount = 0) }
    val saved = container.savedImages.getByIdsChunked(ids)
    trash.begin(saved)
    val removable = saved.filter { container.storage.deleteGalleryUri(it.uri) }
    if (removable.isNotEmpty()) container.savedImages.deleteManyChunked(removable.map { it.id })
    trash.markGone(removable)
    val removedIds = removable.map { it.id }.toSet()
    val failed = saved.filter { it.id !in removedIds }
    clearSelection()
    val sender = if (failed.isEmpty()) null else withContext(Dispatchers.IO) { container.storage.galleryDeleteRequest(failed.map { it.uri }) }
    if (sender != null) {
        _state.update { it.copy(pendingGalleryDelete = PendingGalleryDelete(sender, failed.map { f -> f.id }, removable.size)) }
    } else {
        finishSavedDelete(removable.size, ids.size - removable.size)
    }
}

internal fun LibraryViewModel.finishSavedDelete(removed: Int, failed: Int) {
    trash.release()
    val undo = trash.undoable
    _state.update { it.withMessage(deletionMessage(removed, failed, saved = true), failed > 0, FrameIcons.Trash).copy(undoSavedCount = undo) }
    if (undo > 0) {
        undoJob?.cancel()
        undoJob = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            trash.commit()
            _state.update { it.copy(undoSavedCount = 0) }
        }
    }
}

internal fun LibraryViewModel.undoSavedDelete() {
    if (_state.value.undoSavedCount == 0) return
    undoJob?.cancel()
    viewModelScope.launch {
        _state.update { it.copy(undoSavedCount = 0, message = null, errorMessage = null, messageIcon = null, messageSaved = null) }
        val restored = trash.restoreAll()
        _state.update {
            if (restored > 0) it.withMessage(SavedStrings.restored(restored), false, FrameIcons.Trash)
            else it.withMessage(SavedStrings.restoreFailed(), true)
        }
    }
}
internal fun LibraryViewModel.reconcileSaved(force: Boolean = false) {
    val now = android.os.SystemClock.elapsedRealtime()
    val s = _state.value
    if (reconcileJob?.isActive == true || s.exportProgress != null || s.pendingGalleryDelete != null || s.undoSavedCount > 0) return
    if (!force && lastReconcile != 0L && now - lastReconcile < RECONCILE_MIN_MS) return
    lastReconcile = now
    reconcileJob = viewModelScope.launch {
        val removed = try {
            reconciler.prune()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            com.frameout.core.util.DiagLog.append("library", "reconcile failed: ${e.javaClass.simpleName}: ${e.message}")
            0
        }
        if (removed > 0 && _state.value.undoSavedCount == 0) _state.update { it.withMessage(SavedStrings.pruned(removed), false, FrameIcons.Trash) }
    }
}
