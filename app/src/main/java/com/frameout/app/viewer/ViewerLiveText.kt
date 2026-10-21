package com.frameout.app.viewer

import android.app.Application
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ViewerLiveText(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<ViewerState>,
    private val app: Application,
    private val stopPlay: () -> Unit
) {
    private var job: Job? = null

    fun reset() {
        job?.cancel()
        if (state.value.liveText != LiveTextState()) state.update { it.copy(liveText = LiveTextState()) }
    }

    fun toggle() {
        val s = state.value
        if (s.liveText.active) { reset(); return }
        if (!com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.Insights)) return
        val bitmap = s.bitmap?.takeUnless { it.isRecycled } ?: return
        val frameId = s.current?.id ?: return
        stopPlay()
        job?.cancel()
        state.update { it.copy(liveText = LiveTextState(active = true, scanning = true)) }
        job = scope.launch {
            val result = try {
                com.frameout.core.quality.TextScanner.scanChecked(bitmap)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                com.frameout.core.util.DetectorHealth.failed("text", e)
                com.frameout.core.quality.TextScanResult(emptyList(), listOf("text", "barcodes"))
            }
            state.update { cur ->
                if (cur.current?.id == frameId && cur.liveText.active) cur.copy(liveText = cur.liveText.copy(scanning = false, scanned = true, failed = result.allFailed, words = result.words)) else cur
            }
        }
    }

    fun select(anchor: Int, focus: Int) = state.update { it.copy(liveText = it.liveText.copy(anchor = anchor, focus = focus)) }

    fun tapWord(index: Int) = state.update {
        val lt = it.liveText
        val same = lt.hasSelection && lt.anchor == index && lt.focus == index
        it.copy(liveText = if (same) lt.copy(anchor = -1, focus = -1) else lt.copy(anchor = index, focus = index))
    }

    fun clearSelection() = state.update { it.copy(liveText = it.liveText.copy(anchor = -1, focus = -1)) }

    fun selectAll() = state.update {
        val n = it.liveText.words.size
        if (n == 0) it else it.copy(liveText = it.liveText.copy(anchor = 0, focus = n - 1))
    }

    fun copy() {
        val text = state.value.liveText.selectedText()
        if (text.isBlank()) return
        val clipboard = app.getSystemService(android.content.ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Frame out", text))
        state.update { it.copy(message = com.frameout.core.util.QualityStrings.liveTextCopied(), messageIcon = com.frameout.designsystem.icons.FrameIconsExtra.Copy) }
    }
}
