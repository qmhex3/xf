package com.frameout.app.frames

import com.frameout.core.db.AlbumCover
import com.frameout.core.quality.FocusRegion
import com.frameout.core.quality.FrameTagScanner
import com.frameout.core.quality.FrameTagger
import com.frameout.core.quality.PrefsTagVersion
import com.frameout.core.quality.ScanTiming
import com.frameout.core.quality.TagVersion
import com.frameout.core.quality.ScanReport
import com.frameout.core.quality.ScanVerdict
import com.frameout.core.quality.NormBox
import com.frameout.core.quality.NormBoxCodec
import com.frameout.core.util.AppContainer
import com.frameout.core.util.QualityStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val UI_INTERVAL_MS = 1500L

internal class FramesTags(
    private val scope: CoroutineScope,
    private val container: AppContainer,
    private val state: MutableStateFlow<FramesState>,
    private val projectId: () -> Long,
    private val refresh: suspend (Long) -> Unit
) {
    private var scanJob: Job? = null
    private var watchJob: Job? = null
    private var watchedId = -1L
    private var manualScan = false

    fun open() {
        if (state.value.filter == FrameFilter.Tagged) {
            state.update { it.copy(tagsOpen = true) }
            scope.launch { refreshRefine() }
        } else {
            showAlbums()
        }
    }

    fun showAlbums() {
        state.update { it.copy(filter = FrameFilter.Albums, tagFilter = emptySet(), refineCounts = emptyList(), tagsOpen = false, selected = emptySet(), loading = true) }
        scope.launch { refresh(projectId()); refreshCounts() }
        startScan()
    }

    fun ensureScan() {
        val id = projectId()
        if (id < 0) return
        scope.launch { refreshCounts() }
        if (!watch(id)) startScan()
    }

    private fun watch(id: Long): Boolean {
        if (watchedId == id && watchJob?.isActive == true) return false
        watchJob?.cancel()
        watchedId = id
        watchJob = scope.launch {
            container.projects.observeById(id).map { it?.status }.distinctUntilChanged().collect { onStatus(it) }
        }
        return true
    }

    private fun onStatus(status: String?) {
        if (ScanTiming.mayAutoStart(status)) {
            startScan()
        } else if (ScanTiming.shouldHalt(status, scanJob?.isActive == true, manualScan)) {
            scanJob?.cancel()
        }
    }

    fun close() = state.update { it.copy(tagsOpen = false) }

    fun toggle(tag: String) {
        val next = state.value.tagFilter.toMutableSet()
        if (!next.add(tag)) next.remove(tag)
        applyFilter(next)
    }

    fun clear() = applyFilter(emptySet())

    fun rescan() {
        if (projectId() < 0 || state.value.scanning) return
        scope.launch {
            FrameTagScanner.reset(container.frameTags, projectId())
            state.update { it.copy(tagCounts = emptyList()) }
            startScan(manual = true)
        }
    }

    private fun applyFilter(next: Set<String>) {
        val leaving = next.isEmpty()
        state.update { it.copy(tagFilter = next, filter = if (leaving) FrameFilter.Albums else FrameFilter.Tagged, tagsOpen = it.tagsOpen && !leaving, selected = emptySet(), loading = true, refineCounts = if (leaving) emptyList() else it.refineCounts) }
        scope.launch { refresh(projectId()); refreshRefine() }
    }

    private fun startScan(manual: Boolean = false) {
        val id = projectId()
        if (id < 0 || scanJob?.isActive == true) return
        if (!manual && !FrameTagScanner.autoScanAllowed()) return
        manualScan = manual
        scanJob = scope.launch {
            if (!manual && !ScanTiming.mayAutoStart(container.projects.getById(id)?.status)) return@launch
            runScan(id)
        }
    }

    private suspend fun runScan(id: Long) {
        TagVersion.ensure(PrefsTagVersion(container.prefs), FrameTagger.VERSION) { container.frameTags.clearAllScans() }
        state.update { it.copy(scanning = true, scanDone = 0, scanTotal = 0, scanFailed = 0) }
        try {
            var lastUi = 0L
            val report = FrameTagScanner.scan(container.frameTags, id) { p ->
                val now = System.nanoTime() / 1_000_000L
                if (p.done + p.failed >= p.total || now - lastUi >= UI_INTERVAL_MS) {
                    lastUi = now
                    state.update { it.copy(scanDone = p.done, scanTotal = p.total, scanFailed = p.failed) }
                    refreshCounts()
                    if (state.value.filter == FrameFilter.Tagged) { refresh(id); refreshRefine() }
                }
            }
            reportMessage(report)?.let { text -> state.update { it.copy(message = text, messageIcon = com.frameout.designsystem.icons.FrameIconsExtra.Tag) } }
        } finally {
            state.update { it.copy(scanning = false) }
            refreshCounts()
        }
    }

    private fun reportMessage(report: ScanReport): String? {
        val detector = report.worstDetector ?: "-"
        return when (report.verdict) {
            ScanVerdict.Complete -> null
            ScanVerdict.Failed -> QualityStrings.contentScanAllFailed(report.detectors.keys.sorted().joinToString())
            ScanVerdict.Stopped -> QualityStrings.contentScanStopped(detector)
            ScanVerdict.Partial -> if (report.unavailable.isNotEmpty()) QualityStrings.contentScanPartial(report.partial, detector) else QualityStrings.contentScanFailed(report.incomplete)
        }
    }

    private suspend fun refreshRefine() {
        val tags = state.value.tagFilter.toList()
        if (state.value.filter != FrameFilter.Tagged || tags.isEmpty()) return
        val counts = runCatching { container.frameTags.coTagCounts(projectId(), tags, tags.size) }.getOrDefault(emptyList())
        state.update { it.copy(refineCounts = counts) }
    }

    suspend fun cover(tag: String): AlbumCover? = runCatching {
        val id = projectId()
        val row = container.frameTags.coverTagFor(id, tag)
        val boxed = row?.let { r -> container.frames.getActiveById(r.frameId)?.let { it to r } }
        if (boxed != null) {
            AlbumCover(boxed.first, FocusRegion.largest(NormBoxCodec.decode(boxed.second.boxes)))
        } else {
            container.frameTags.coverFor(id, tag)?.let { AlbumCover(it, null) }
        }
    }.getOrNull()

    suspend fun focus(frameId: Long, tags: Set<String>): NormBox? {
        if (tags.isEmpty()) return null
        return runCatching {
            val rows = container.frameTags.boxesFor(frameId, tags.toList())
            FocusRegion.union(rows.mapNotNull { FocusRegion.largest(NormBoxCodec.decode(it.boxes)) })
        }.getOrNull()
    }

    private suspend fun refreshCounts() {
        val counts = runCatching { container.frameTags.tagCounts(projectId()) }.getOrDefault(emptyList())
        state.update { it.copy(tagCounts = counts) }
    }
}
