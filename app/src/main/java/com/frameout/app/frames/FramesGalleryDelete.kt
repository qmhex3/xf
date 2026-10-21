package com.frameout.app.frames

import com.frameout.app.deletionMessage
import com.frameout.app.library.PendingGalleryDelete
import com.frameout.core.db.deleteManyChunked
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.util.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SQL_CHUNK = 500

internal class FramesGalleryDelete(
    private val scope: CoroutineScope,
    private val container: AppContainer,
    private val state: MutableStateFlow<FramesState>
) {
    fun undo() = scope.launch {
        val ids = state.value.undoGalleryIds
        state.update { it.copy(message = null, undoGalleryIds = emptyList()) }
        val failed = mutableListOf<Pair<Long, String>>()
        val removed = withContext(Dispatchers.IO) { removeSaved(ids, failed) }
        val sender = if (failed.isEmpty()) null else withContext(Dispatchers.IO) { container.storage.galleryDeleteRequest(failed.map { it.second }) }
        if (sender != null) {
            state.update { it.copy(pendingGalleryDelete = PendingGalleryDelete(sender, failed.map { f -> f.first }, removed)) }
        } else {
            state.update { it.copy(message = deletionMessage(removed, ids.size - removed), messageIcon = com.frameout.designsystem.icons.FrameIcons.Trash) }
        }
    }
    fun onResult(approved: Boolean) = scope.launch {
        val pending = state.value.pendingGalleryDelete ?: return@launch
        state.update { it.copy(pendingGalleryDelete = null) }
        val all = container.savedImages.getByIdsChunked(pending.ids)
        val gone = withContext(Dispatchers.IO) { all.filter { container.storage.deleteGalleryUri(it.uri) } }
        if (gone.isNotEmpty()) container.savedImages.deleteManyChunked(gone.map { it.id })
        val goneIds = gone.map { it.id }.toSet()
        val remaining = all.filter { it.id !in goneIds }
        val removed = pending.alreadyRemoved + gone.size
        if (approved && gone.isNotEmpty() && remaining.isNotEmpty()) {
            val next = withContext(Dispatchers.IO) { container.storage.galleryDeleteRequest(remaining.map { it.uri }) }
            if (next != null) {
                state.update { it.copy(pendingGalleryDelete = PendingGalleryDelete(next, remaining.map { r -> r.id }, removed)) }
                return@launch
            }
        }
        val total = pending.alreadyRemoved + pending.ids.size
        state.update { it.copy(message = deletionMessage(removed, total - removed), messageIcon = com.frameout.designsystem.icons.FrameIcons.Trash) }
    }
    private suspend fun removeSaved(ids: List<Long>, failed: MutableList<Pair<Long, String>> = mutableListOf()): Int {
        var removed = 0
        ids.chunked(SQL_CHUNK).forEach { chunk ->
            val saved = container.savedImages.getByIdsChunked(chunk)
            val gone = saved.filter { container.storage.deleteGalleryUri(it.uri) }
            if (gone.isNotEmpty()) container.savedImages.deleteManyChunked(gone.map { it.id })
            val goneIds = gone.map { it.id }.toSet()
            saved.filter { it.id !in goneIds }.forEach { failed += it.id to it.uri }
            removed += gone.size
        }
        return removed
    }
}
