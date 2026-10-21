package com.frameout.app.frames

import com.frameout.core.quality.FrameSharpnessAnalyzer
import java.io.File
import com.frameout.app.deletionMessage
import com.frameout.core.util.logged
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.frameout.app.FrameOutApp
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.getByIdsChunked
import com.frameout.core.db.permanentDeleteAtomic
import com.frameout.core.db.setFavoriteAtomic
import com.frameout.core.edit.FrameEdit
import com.frameout.core.edit.toEdit
import kotlinx.coroutines.flow.map
import com.frameout.core.storage.FrameExporter
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val GALLERY_CONFIRM_THRESHOLD = 200
internal const val GALLERY_UNDO_WINDOW_MS = 10000L

class FramesViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as FrameOutApp).container
    private val exporter = FrameExporter(app, container.storage, container.savedImages, container.frameEdits, container.projects, container.videoIndex)
    private val queries = FramesQueries(container)
    private val _state = MutableStateFlow(FramesState())
    val state: StateFlow<FramesState> = _state.asStateFlow()
    private var projectId = -1L

    fun load(id: Long, heavy: Boolean = true) {
        projectId = id
        viewModelScope.launch { refresh(id, heavy); if (heavy) tags.ensureScan() }
    }
    fun postMessage(text: String, busy: Boolean = false, @androidx.annotation.DrawableRes icon: Int? = null) = _state.update { it.copy(message = text, busyMessage = if (busy) text else null, messageSaved = null, messageIcon = icon, exportSaved = null) }
    fun postBusy(text: String) = postMessage(text, busy = true)
    fun clearBusy() = _state.update { if (it.messageBusy) it.copy(message = null, busyMessage = null) else it }
    val exportRunning: Boolean get() = _state.value.messageBusy
    private var exportJob: kotlinx.coroutines.Job? = null
    private var exportLeaveArmed = false

    internal fun exportStarted(job: kotlinx.coroutines.Job?) {
        exportJob = job
        exportLeaveArmed = false
    }

    internal fun exportEnded() {
        exportJob = null
        exportLeaveArmed = false
    }

    fun holdBack(): Boolean {
        if (!exportRunning) return false
        if (exportLeaveArmed) {
            exportJob?.cancel()
            return false
        }
        exportLeaveArmed = true
        postBusy(com.frameout.core.util.ClipStrings.saveWait())
        return true
    }
    fun postExport(outcome: ExportOutcome) = _state.update { it.copy(message = outcome.message.takeIf { outcome.saved == null }, busyMessage = null, messageIcon = null, exportSaved = outcome.saved) }
    fun clearMessage() = _state.update { if (it.messageBusy) it else it.copy(message = null, busyMessage = null, messageSaved = null, messageIcon = null, undoGalleryIds = emptyList()) }
    fun clearUndoDelete() = _state.update { it.copy(undoDelete = emptyList()) }
    fun undoDelete() {
        val ids = _state.value.undoDelete
        if (ids.isEmpty()) return
        _state.update { it.copy(undoDelete = emptyList()) }
        viewModelScope.launch { container.editHistory.restoreMany(ids); refresh(projectId) }
    }
    fun clearExport() = _state.update { it.copy(exportSaved = null) }
    fun setFilter(filter: FrameFilter) {
        if (filter == FrameFilter.Tagged) return
        if (filter == FrameFilter.Sharpest) { showSharpest(); return }
        if (filter == FrameFilter.Albums) {
            if (_state.value.filter != FrameFilter.Albums) { tags.showAlbums() }
            return
        }
        if (_state.value.filter == filter && _state.value.tagFilter.isEmpty()) return
        _state.update { it.copy(filter = filter, tagFilter = emptySet(), selected = emptySet(), loading = true) }
        viewModelScope.launch { refresh(projectId) }
    }
    private val tags = FramesTags(viewModelScope, container, _state, { projectId }, ::refresh)
    private val edits = FramesEdits(viewModelScope, container, _state)
    private val crop = FramesCropAll(viewModelScope, container, _state, getApplication<Application>(), ::selectedFrames)
    fun openTools() = _state.update { it.copy(toolsOpen = true) }
    fun closeTools() = _state.update { it.copy(toolsOpen = false) }
    fun openTags() { if (com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.Insights)) tags.open() }
    fun closeTags() = tags.close()
    fun toggleTag(tag: String) = tags.toggle(tag)
    fun openAlbum(tag: String) = tags.toggle(tag)
    suspend fun albumCover(tag: String) = tags.cover(tag)
    suspend fun albumFocus(frameId: Long, tagSet: Set<String>) = tags.focus(frameId, tagSet)
    fun clearTags() = tags.clear()
    fun rescanTags() { if (com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.Insights)) tags.rescan() }
    fun openCropAll() { if (com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.EditTools)) crop.open() }
    fun updateCropAll(l: Float, t: Float, r: Float, b: Float) = crop.update(l, t, r, b)
    fun resetCropAll() = crop.reset()
    fun cancelCropAll() = crop.cancel()
    fun applyCropAll() = crop.apply()
    fun toggleIntegrity() = _state.update { if (it.integrity == null) it else it.copy(integrityOpen = !it.integrityOpen) }
    fun toggleFit() = _state.update { it.copy(fit = if (it.fit == GridFit.Fill) GridFit.Fit else GridFit.Fill) }
    fun toggleSelect(id: Long) = _state.update { it.toggle(id) }
    fun selectAll() {
        viewModelScope.launch {
            val ids = queries.ids(projectId, _state.value)
            _state.update { it.copy(selected = ids.toSet()) }
        }
    }
    fun clearSelection() = _state.update { it.clearSelection() }
    fun showSharpest() {
        if (projectId < 0 || _state.value.filter == FrameFilter.Sharpest) return
        if (!com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.SmartExtract)) return
        _state.update { it.copy(filter = FrameFilter.Sharpest, tagFilter = emptySet(), selected = emptySet(), loading = true) }
        viewModelScope.launch { loadSharpest() }
    }
    private suspend fun loadSharpest() {
        val total = container.frames.countAll(projectId)
        if (!SharpestCache.isValid(getApplication<Application>(), projectId, total)) {
            _state.update { it.copy(analyzing = true) }
            val fresh = runCatching { FrameSharpnessAnalyzer.bestIds(container.frames, projectId) }
            fresh.onSuccess {
                container.sharpest.replace(projectId, it)
                SharpestCache.mark(getApplication<Application>(), projectId, total)
            }
            fresh.onFailure { container.sharpest.replace(projectId, emptyList()) }
        }
        _state.update { it.copy(sharpestVersion = it.sharpestVersion + 1, analyzing = false) }
        refresh(projectId)
    }
    fun pickImportant() { if (com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.SmartExtract)) analyze(FrameIconsExtra.Sparkle) { importantSelection(container.frames, container.frameTags, projectId) } }
    fun pickDuplicates() { if (com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.SmartExtract)) analyze(FrameIconsExtra.Duplicate) { duplicateSelection(container.frames, projectId) } }
    fun pickNoPeople() { if (com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.SmartExtract)) analyze(FrameIconsExtra.PersonOff) { noPeopleSelection(container.frames, projectId) } }
    private fun analyze(@androidx.annotation.DrawableRes icon: Int, job: suspend () -> Pair<Set<Long>, String>) = viewModelScope.launch {
        if (_state.value.analyzing || projectId < 0) return@launch
        _state.update { it.analysisStarted() }
        val (ids, text) = job()
        _state.update { it.analysisDone(ids, text, icon) }
    }
    fun rename(name: String) {
        if (projectId < 0 || name.isBlank()) return
        viewModelScope.launch {
            val value = name.trim()
            container.projects.rename(projectId, value, System.currentTimeMillis())
            com.frameout.engine.work.ExtractNotifications.invalidateTitle(projectId)
            _state.update { s -> s.copy(project = s.project?.copy(name = value)) }
        }
    }
    fun softDeleteSelected() {
        val ids = _state.value.selected.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            container.editHistory.deleteMany(ids)
            _state.update { it.clearSelection().copy(message = null, undoDelete = ids) }
            refresh(projectId)
        }
    }
    fun permanentDeleteSelected() {
        val ids = _state.value.selected.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val frames = container.frames.getByIdsChunked(ids)
            val removable = withContext(Dispatchers.IO) { frames.filter { deleteFrameFiles(it) } }
            if (removable.isNotEmpty()) container.database.permanentDeleteAtomic(removable.map { it.id })
            _state.update { it.clearSelection().copy(message = deletionMessage(removable.size, ids.size - removable.size), messageIcon = FrameIcons.Trash) }
            refresh(projectId)
        }
    }
    fun emptyTrash() {
        viewModelScope.launch {
            val ids = container.frames.listDeletedIds(projectId)
            if (ids.isEmpty()) return@launch
            val frames = container.frames.getByIdsChunked(ids)
            val removable = withContext(Dispatchers.IO) { frames.filter { deleteFrameFiles(it) } }
            if (removable.size == frames.size) {
                container.frames.emptyTrash(projectId)
            } else if (removable.isNotEmpty()) {
                container.database.permanentDeleteAtomic(removable.map { it.id })
            }
            _state.update { it.copy(message = deletionMessage(removable.size, frames.size - removable.size), messageIcon = FrameIcons.Trash) }
            refresh(projectId)
        }
    }
    fun restoreSelected() = mutateSelection { ids -> container.editHistory.restoreMany(ids) }
    fun toggleFavoriteSelected() {
        viewModelScope.launch {
            val frames = selectedFrames()
            if (frames.isEmpty()) return@launch
            val value = !frames.all { it.isFavorite }
            container.database.setFavoriteAtomic(frames.map { it.id }, value)
            refresh(projectId)
        }
    }
    fun toggleFavorite(id: Long, currently: Boolean) = viewModelScope.launch { container.frames.setFavorite(id, !currently); refresh(projectId) }
    fun applyRotate90() = edits.transform { it.rotate90() }
    fun applyFlipH() = edits.transform { it.toggleFlipH() }
    fun applyFlipV() = edits.transform { it.toggleFlipV() }
    fun undoSelected() = edits.history { container.editHistory.undoMany(it) }
    fun redoSelected() = edits.history { container.editHistory.redoMany(it) }
    fun saveSelected() {
        val count = _state.value.selected.size
        if (count > GALLERY_CONFIRM_THRESHOLD) _state.update { it.copy(confirmGallerySave = count) } else confirmGallerySave()
    }
    fun dismissGallerySave() = _state.update { it.copy(confirmGallerySave = 0) }
    private val gallery = FramesGallerySave(viewModelScope, getApplication<Application>(), exporter, _state, ::selectedFrames)
    fun cancelGallerySave() = gallery.cancel()
    fun confirmGallerySave() = gallery.confirm()
    private val galleryDelete = FramesGalleryDelete(viewModelScope, container, _state)
    fun undoGallerySave() = galleryDelete.undo()
    fun onGalleryDeleteResult(approved: Boolean) = galleryDelete.onResult(approved)
    fun searchSelected(context: android.content.Context, engine: com.frameout.core.search.SearchEngine) {
        viewModelScope.launch {
            val frame = selectedFrames().firstOrNull() ?: return@launch
            val busyText = com.frameout.core.util.SearchStrings.opening()
            postMessage(busyText, busy = true)
            val message = com.frameout.app.search.ImageSearchLauncher.run(context, exporter, engine, frame)
            if (message != null) _state.update { it.copy(message = message, busyMessage = null, messageIcon = null) }
            else {
                kotlinx.coroutines.delay(com.frameout.app.search.SEARCH_HANDOFF_MS)
                _state.update { if (it.busyMessage == busyText) it.copy(message = null, busyMessage = null) else it }
            }
        }
    }
    fun shareIntent(onReady: (Intent) -> Unit) = viewModelScope.launch {
        val frames = selectedFrames()
        if (frames.size > FrameExporter.MAX_SHARE_FRAMES) _state.update { it.copy(message = FrameStrings.shareLimited(FrameExporter.MAX_SHARE_FRAMES), messageIcon = FrameIcons.Share) }
        exporter.shareIntent(frames)?.let(onReady)
    }
    fun framesForSelection(onReady: (List<FrameEntity>) -> Unit) = viewModelScope.launch { onReady(selectedFrames()) }
    fun framesForExport(onReady: (List<FrameEntity>) -> Unit) = viewModelScope.launch { onReady(selectedOrAllFrames()) }
    private var reassembleJob: kotlinx.coroutines.Job? = null
    fun reassembleVideo() {
        if (!com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.AdvancedExport)) return
        if (_state.value.reassembling) {
            reassembleJob?.cancel()
            return
        }
        reassembleJob = viewModelScope.launch {
            val project = _state.value.project ?: return@launch
            val frames = selectedOrAllFrames()
            if (_state.value.reassembling || frames.isEmpty()) return@launch
            _state.update { it.copy(reassembling = true, message = null) }
            try {
                val outcome = FramesExportAction(getApplication()).video(project, frames)
                _state.update { it.copy(message = outcome.message.takeIf { outcome.saved == null }, messageIcon = null, exportSaved = outcome.saved) }
            } finally {
                _state.update { it.copy(reassembling = false) }
            }
        }
    }
    fun editsFlow(id: Long): Flow<Map<Long, FrameEdit>> = container.frameEdits.observeProject(id).map { list ->
        list.map { it.toEdit() }.filter { !it.isIdentity }.associateBy { it.frameId }
    }
    fun pagingFlow(): Flow<PagingData<FrameEntity>> = Pager(PagingConfig(60, prefetchDistance = 30, enablePlaceholders = true)) {
        queries.paging(projectId, _state.value)
    }.flow.cachedIn(viewModelScope)
    private suspend fun refresh(id: Long, withIntegrity: Boolean = true) {
        val count = queries.count(id, _state.value)
        val project = container.projects.getById(id)
        val all = container.frames.countActive(id)
        val favorites = container.frames.countFavorites(id)
        val deleted = container.frames.countDeleted(id)
        val sharp = if (_state.value.sharpestVersion > 0) container.sharpest.count(id) else _state.value.sharpestCount
        _state.update { it.copy(project = project, frameCount = count, allCount = all, favoriteCount = favorites, deletedCount = deleted, sharpestCount = sharp, loading = false) }
        if (withIntegrity && project != null && _state.value.integrity == null) {
            val report = runCatching { IntegrityLoader.load(getApplication(), project) }.logged("FramesViewModel").getOrNull()
            _state.update { it.copy(integrity = report) }
        }
    }
    private suspend fun selectedOrAllFrames(): List<FrameEntity> {
        val selected = selectedFrames()
        if (selected.isNotEmpty()) return selected
        val out = ArrayList<FrameEntity>()
        var offset = 0
        while (true) {
            val page = container.frames.pageActive(projectId, 500, offset)
            if (page.isEmpty()) break
            out += page
            offset += page.size
        }
        return out
    }
    private suspend fun selectedFrames(): List<FrameEntity> {
        val ids = _state.value.selected.toList()
        return if (ids.isEmpty()) emptyList() else container.frames.getByIdsChunked(ids)
    }
    private fun mutateSelection(block: suspend (List<Long>) -> Unit) {
        val ids = _state.value.selected.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch { block(ids); _state.update { it.clearSelection() }; refresh(projectId) }
    }
    private fun deleteFrameFiles(frame: com.frameout.core.db.FrameEntity): Boolean =
        listOfNotNull(frame.thumbnailPath, frame.fullPath, com.frameout.core.quality.AnalysisSource.copyPath(frame.thumbnailPath, frame.index)).all { path -> File(path).let { !it.exists() || it.delete() } }
}
