package com.frameout.app.viewer
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frameout.app.FrameOutApp
import com.frameout.core.db.FrameEditEntity
import com.frameout.core.db.FrameEntity
import com.frameout.core.edit.FrameEdit
import com.frameout.core.storage.FrameExporter
import com.frameout.core.util.EditedThumb
import com.frameout.core.util.ThumbCache
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class ViewerViewModel(app: Application) : AndroidViewModel(app) {
    internal val c = (app as FrameOutApp).container
    private val exporter = FrameExporter(app, c.storage, c.savedImages, c.frameEdits, c.projects, c.videoIndex)
    private val loader = FrameBitmapLoader(app)
    private val editHistory = c.editHistory
    internal val _state = MutableStateFlow(ViewerState())
    private val player = ViewerPlayer(viewModelScope, _state, c.frames, { loadFrame(it, seamless = true) }, ::refreshNeighbors)
    private val export = ViewerExport(viewModelScope, exporter, _state, app)
    internal val tools = ViewerToolEditor(viewModelScope, _state, loader, editHistory, { sourceUri }, { loadFrame(_state.value.current) })
    val state: StateFlow<ViewerState> = _state.asStateFlow()
    private var sourceUri: Uri? = null
    private var loadGeneration = 0L
    private var retired: Bitmap? = null
    private val cache = WindowCache<Bitmap>(
        budgetBytes = (Runtime.getRuntime().maxMemory() / 6L).coerceIn(48L * 1024L * 1024L, 128L * 1024L * 1024L),
        maxEntryBytes = MAX_PREFETCH_BYTES.toLong(),
        sizeOf = { it.allocationByteCount.toLong() },
        drop = { it.takeUnless { b -> b.isRecycled }?.recycle() }
    )
    private val inFlight = HashMap<Long, Job>()
    private val decodeGate = Semaphore(2)
    private var windowJob: Job? = null
    private var navJob: Job? = null
    private var pendingSteps = 0
    internal var direction = 1
    private val screenPx = app.resources.displayMetrics.let { minOf(it.widthPixels, it.heightPixels) }
    internal var replaceSavedId: Long? = null
    internal var replaceFrameId: Long? = null
    internal var savedIds: List<Long> = emptyList()
    internal var savedIndex = 0
    internal val savedDelete = ViewerSavedDelete(c.storage, c.savedImages, com.frameout.core.storage.GalleryBackup(app))

    fun open(projectId: Long, frameId: Long, savedId: Long? = null) {
        replaceSavedId = savedId
        replaceFrameId = savedId?.let { frameId }
        savedIds = if (savedId == null) emptyList() else SavedPlaylist.ids.takeIf { savedId in it } ?: listOf(savedId)
        savedIndex = savedId?.let { savedIds.indexOf(it) } ?: 0
        _state.update { it.copy(savedMode = savedId != null) }
        viewModelScope.launch {
            sourceUri = c.projects.getById(projectId)?.sourceUri?.let(Uri::parse)
            loadFrame(c.frames.getActiveById(frameId))
        }
    }
    fun openTools(tool: EditTool = EditTool.Adjust) { stopPlay(); resetLiveText(); tools.open(tool) }
    fun togglePlay() { resetLiveText(); player.toggle() }
    fun stopPlay() = player.stop()
    private fun refreshPosition(frame: FrameEntity) {
        if (savedIds.isNotEmpty()) {
            _state.update { if (it.current?.id == frame.id) it.copy(position = savedIndex + 1, total = savedIds.size) else it }
            return
        }
        viewModelScope.launch {
            val (before, total) = withContext(Dispatchers.IO) {
                c.frames.countActiveBefore(frame.projectId, frame.index) to c.frames.countActive(frame.projectId)
            }
            _state.update { if (it.current?.id == frame.id) it.copy(position = before + 1, total = total) else it }
        }
    }
    private suspend fun refreshNeighbors() {
        val frame = _state.value.current ?: return
        val neighbors = neighborsFor(frame)
        if (_state.value.current?.id != frame.id || _state.value.playing) return
        _state.update { it.withNeighbors(neighbors) }
    }
    private suspend fun neighborsFor(frame: FrameEntity): ViewerNeighbors {
        if (savedIds.isEmpty()) return ViewerNeighbors.load(c.frames, editHistory, frame, screenPx)
        val (previous, next) = withContext(Dispatchers.IO) { savedFrameFrom(savedIndex, -1)?.second to savedFrameFrom(savedIndex, 1)?.second }
        return ViewerNeighbors.of(editHistory, previous, next, screenPx)
    }

    internal suspend fun savedFrameFrom(from: Int, dir: Int): Pair<Int, FrameEntity>? {
        var i = from + dir
        while (i in savedIds.indices) {
            val frame = c.savedImages.getById(savedIds[i])?.sourceFrameId?.let { c.frames.getActiveById(it) }
            if (frame != null) return i to frame
            i += dir
        }
        return null
    }

    private suspend fun stepSaved(steps: Int) {
        val dir = if (steps > 0) 1 else -1
        var target: Pair<Int, FrameEntity>? = null
        var from = savedIndex
        repeat(kotlin.math.abs(steps)) {
            val next = withContext(Dispatchers.IO) { savedFrameFrom(from, dir) } ?: return@repeat
            target = next
            from = next.first
        }
        val (index, frame) = target ?: return
        savedIndex = index
        replaceSavedId = savedIds[index]
        replaceFrameId = frame.id
        loadFrame(frame)
    }

    fun goTo(delta: Int) {
        stopPlay()
        if (delta == 0) return
        pendingSteps += delta
        if (navJob?.isActive == true) return
        navJob = viewModelScope.launch {
            while (pendingSteps != 0) {
                val steps = pendingSteps
                pendingSteps = 0
                if (savedIds.isNotEmpty()) {
                    direction = if (steps > 0) 1 else -1
                    stepSaved(steps)
                    continue
                }
                val base = _state.value.current ?: break
                direction = if (steps > 0) 1 else -1
                val target = withContext(Dispatchers.IO) {
                    if (steps > 0) c.frames.nextActiveList(base.projectId, base.index, steps)
                    else c.frames.previousActiveList(base.projectId, base.index, -steps)
                }.lastOrNull()
                if (target != null) loadFrame(target)
            }
        }
    }
    private val live = ViewerLiveText(viewModelScope, _state, app) { stopPlay() }
    private fun resetLiveText() = live.reset()
    fun toggleLiveText() = live.toggle()
    fun liveSelect(anchor: Int, focus: Int) = live.select(anchor, focus)
    fun liveTapWord(index: Int) = live.tapWord(index)
    fun liveClearSelection() = live.clearSelection()
    fun liveSelectAll() = live.selectAll()
    fun liveCopy() = live.copy()
    fun undoEdit() = stepHistory { editHistory.undo(it) }
    fun redoEdit() = stepHistory { editHistory.redo(it) }
    private fun stepHistory(op: suspend (Long) -> FrameEditEntity?) {
        val frame = _state.value.current ?: return
        viewModelScope.launch { op(frame.id)?.let { applyHistory(it) }; loadFrame(_state.value.current) }
    }
    fun softDelete() {
        stopPlay()
        if (_state.value.savedMode) { deleteSaved(); return }
        val frame = _state.value.current ?: return
        viewModelScope.launch {
            c.editHistory.delete(frame.id)
            _state.update { it.copy(deletedStack = it.deletedStack + frame.id) }
            val next = c.frames.nextActive(frame.projectId, frame.index) ?: c.frames.previousActive(frame.projectId, frame.index)
            loadFrame(next)
        }
    }
    fun undoDelete() {
        val id = _state.value.deletedStack.lastOrNull() ?: return
        stopPlay()
        viewModelScope.launch {
            if (c.editHistory.undo(id) == null) c.editHistory.restore(id)
            _state.update { it.copy(deletedStack = it.deletedStack.dropLast(1)) }
            loadFrame(c.frames.getActiveById(id))
        }
    }
    fun save() {
        val frame = _state.value.current ?: return
        val savedId = replaceSavedId
        if (savedId != null && frame.id == replaceFrameId) export.replaceSaved(savedId, frame) else export.save(frame)
    }
    fun shareIntent(onReady: (Intent) -> Unit) = _state.value.current?.let { export.share(it, onReady) } ?: Unit
    fun search(context: android.content.Context, engine: com.frameout.core.search.SearchEngine) = _state.value.current?.let { export.search(context, engine, it) } ?: Unit
    private fun applyHistory(entity: FrameEditEntity) {
        _state.update { it.withEdit(fromEntity(entity)) }
    }
    internal suspend fun loadFrame(frame: FrameEntity?, seamless: Boolean = false) {
        if (seamless && frame != null) {
            loadSeamless(frame)
            return
        }
        val generation = ++loadGeneration
        windowJob?.cancel()
        resetLiveText()
        val before = _state.value
        val old = before.bitmap
        if (frame == null) { _state.update { it.cleared() }; old?.takeUnless { it.isRecycled }?.recycle(); return }
        val edit = editHistory.current(frame.id)?.let(::fromEntity) ?: FrameEdit(frame.id)
        val ready = cache.take(frame.id, edit)
        val waiting = ready == null && inFlight.containsKey(frame.id)
        val sharpPlaceholder = if (ready == null) {
            EditedThumb.peekSharp(frame.fullPath, edit, screenPx) ?: neighborThumb(before, frame)
        } else null
        val placeholder = sharpPlaceholder ?: if (ready == null && edit.isIdentity) placeholderFor(frame) else null
        _state.update { it.showing(frame, edit, ready, placeholder, sharpPlaceholder != null) }
        keepOrRecycle(before.current, before.edit, old, frame)
        retired?.takeUnless { it.isRecycled }?.recycle()
        retired = null
        refreshPosition(frame)
        val neighbors = viewModelScope.async { neighborsFor(frame) }
        viewModelScope.launch {
            val found = neighbors.await()
            if (generation == loadGeneration) _state.update { if (it.current?.id == frame.id) it.withNeighbors(found) else it }
        }
        if (ready == null) launchWindow(frame, full = false)
        if (ready == null && !loadMissing(frame, edit, waiting, placeholder, sharpPlaceholder != null, generation, neighbors)) return
        launchWindow(frame)
        val detections = withContext(Dispatchers.IO) { ViewerDetections.load(c.frameTags, frame, edit) }
        if (generation == loadGeneration) _state.update { it.copy(detections = detections) }
    }

    private suspend fun loadMissing(
        frame: FrameEntity,
        edit: FrameEdit,
        waiting: Boolean,
        placeholder: Bitmap?,
        sharp: Boolean,
        generation: Long,
        neighbors: kotlinx.coroutines.Deferred<*>
    ): Boolean {
        if (placeholder != null && !sharp) upgradePlaceholder(frame, generation)
        val bmp = (if (waiting) awaitPrefetch(frame.id, edit) else null)
            ?: withContext(Dispatchers.IO) { loader.load(frame, sourceUri, edit) }
        if (generation != loadGeneration) { bmp?.takeUnless { it.isRecycled }?.recycle(); neighbors.cancel(); return false }
        if (bmp == null) {
            _state.update { it.copy(loading = false, message = com.frameout.core.util.FrameStrings.viewerLoadFailed(), busyMessage = null, messageSaved = null, messageIcon = null) }
            return true
        }
        _state.update { it.copy(bitmap = bmp, placeholder = null, placeholderSharp = false, loading = false) }
        return true
    }

    private fun neighborThumb(before: ViewerState, frame: FrameEntity): Bitmap? = when (frame.id) {
        before.next?.id -> before.nextBitmap
        before.previous?.id -> before.previousBitmap
        else -> null
    }?.takeUnless { it.isRecycled }

    private suspend fun placeholderFor(frame: FrameEntity): Bitmap? {
        val path = frame.thumbnailPath ?: frame.fullPath
        return ThumbCache.peek(path) ?: withContext(Dispatchers.IO) { ThumbCache.getShared(path) }
    }

    private fun upgradePlaceholder(frame: FrameEntity, generation: Long) {
        viewModelScope.launch {
            val sharp = ThumbCache.loadSharp(frame.fullPath, screenPx) ?: return@launch
            if (generation != loadGeneration) return@launch
            _state.update { if (it.current?.id == frame.id && it.bitmap == null) it.copy(placeholder = sharp, placeholderSharp = true) else it }
        }
    }

    private suspend fun awaitPrefetch(id: Long, edit: FrameEdit): Bitmap? {
        inFlight[id]?.join()
        return cache.take(id, edit)
    }

    private fun keepOrRecycle(frame: FrameEntity?, edit: FrameEdit, bitmap: Bitmap?, now: FrameEntity) {
        bitmap ?: return
        if (bitmap.isRecycled) return
        if (frame == null || frame.id == now.id) bitmap.recycle() else cache.put(frame.id, edit, bitmap)
    }

    private fun launchWindow(frame: FrameEntity, full: Boolean = true) {
        windowJob?.cancel()
        val dir = direction
        windowJob = viewModelScope.launch {
            val (ahead, behind) = withContext(Dispatchers.IO) {
                val forward = c.frames.nextActiveList(frame.projectId, frame.index, if (dir > 0) WARM_AHEAD else WARM_BEHIND)
                val backward = c.frames.previousActiveList(frame.projectId, frame.index, if (dir > 0) WARM_BEHIND else WARM_AHEAD)
                if (dir > 0) forward to backward else backward to forward
            }
            val fullList = (listOfNotNull(ahead.getOrNull(0), behind.getOrNull(0)) + ahead.drop(1).take(FULL_AHEAD - 1)).distinctBy { it.id }
            val order = (ahead.take(1) + behind.take(1) + ahead.drop(1) + behind.drop(1)).distinctBy { it.id }
            if (full) {
                val fullIds = fullList.map { it.id }
                cache.setWindow(fullIds)
                inFlight.filterKeys { it !in fullIds }.values.forEach { it.cancel() }
            }
            order.take(2).forEach { ThumbCache.loadSharp(it.fullPath, screenPx) }
            if (full) fullList.forEach(::startPrefetch)
            order.drop(2).forEach { ThumbCache.loadSharp(it.fullPath, screenPx) }
        }
    }

    private fun startPrefetch(frame: FrameEntity) {
        if (cache.contains(frame.id) || inFlight.containsKey(frame.id)) return
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val self = coroutineContext.job
            try {
                val edit = editHistory.current(frame.id)?.let(::fromEntity) ?: FrameEdit(frame.id)
                val bmp = decodeGate.withPermit { withContext(Dispatchers.IO) { loader.load(frame, sourceUri, edit) } } ?: return@launch
                if (cache.wants(frame.id)) cache.put(frame.id, edit, bmp) else bmp.recycle()
            } finally {
                if (inFlight[frame.id] === self) inFlight.remove(frame.id)
            }
        }
        inFlight[frame.id] = job
        job.start()
    }

    override fun onCleared() {
        player.stop()
        savedDelete.commit()
        navJob?.cancel()
        windowJob?.cancel()
        inFlight.values.forEach { it.cancel() }
        inFlight.clear()
        cache.clear()
        _state.value.bitmap?.takeUnless { it.isRecycled }?.recycle()
        retired?.takeUnless { it.isRecycled }?.recycle()
        retired = null
        super.onCleared()
    }

    private suspend fun loadSeamless(frame: FrameEntity) {
        val generation = ++loadGeneration
        resetLiveText()
        val uri = sourceUri
        val edit = editHistory.current(frame.id)?.let(::fromEntity) ?: FrameEdit(frame.id)
        direction = 1
        val bmp = cache.take(frame.id, edit)
            ?: (if (inFlight.containsKey(frame.id)) awaitPrefetch(frame.id, edit) else null)
            ?: withContext(Dispatchers.IO) { loader.load(frame, uri, edit) }
        if (generation != loadGeneration) { bmp?.takeUnless { it.isRecycled }?.recycle(); return }
        val old = _state.value.bitmap
        if (bmp == null) {
            _state.update { it.copy(current = frame, edit = edit) }
            return
        }
        _state.update { it.copy(current = frame, bitmap = bmp, placeholder = null, loading = false, edit = edit, detections = emptyList(), previous = null, next = null, previousBitmap = null, nextBitmap = null) }
        retired?.takeUnless { it.isRecycled }?.recycle()
        retired = old
        refreshPosition(frame)
        launchWindow(frame)
    }
}
