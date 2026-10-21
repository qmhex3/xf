package com.frameout.engine.extract

import com.frameout.codecpack.FfmpegBridge
import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureException
import com.frameout.engine.index.FfmpegIndexManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class IndexView(val count: Int, val lastPtsUs: Long, val complete: Boolean)

internal class StagedIndex(
    private val manager: FfmpegIndexManager,
    private val projectId: Long,
    private val path: String,
    private val shouldAbort: () -> Boolean,
    private val onComplete: suspend (Int) -> Unit
) {
    private val _state = MutableStateFlow(IndexView(0, -1L, false))
    val state: StateFlow<IndexView> = _state.asStateFlow()

    fun markComplete(table: LongArray, count: Int) {
        _state.value = IndexView(count, if (table.isEmpty()) -1L else table[table.size - STRIDE], true)
    }

    suspend fun first(): Boolean {
        val stage = manager.importPrefix(projectId, path, FIRST_PACKETS) ?: return false
        accept(stage.table, stage.complete)
        return true
    }

    suspend fun finish() {
        if (_state.value.complete) return
        val mid = manager.importPrefix(projectId, path, MID_PACKETS)
        if (mid != null) {
            accept(mid.table, mid.complete)
            if (mid.complete) return
        }
        var scanned: LongArray? = null
        val total = manager.importFromNative(projectId, path, shouldAbort) { scanned = it }
        val table = scanned
        if (total <= 0 || table == null) {
            throw MediaFailureException(FfmpegBridge.lastFailure(path, MediaFailure.CorruptData), "FFmpeg could not build the video index")
        }
        accept(table, true)
    }

    private suspend fun accept(table: LongArray, complete: Boolean) {
        FfmpegBridge.nativeSetSessionTable(path, table)
        val count = table.size / STRIDE
        _state.value = IndexView(count, table[table.size - STRIDE], complete)
        if (complete) onComplete(count)
    }

    private companion object {
        const val STRIDE = 4
        const val FIRST_PACKETS = 2_000
        const val MID_PACKETS = 20_000
    }
}

internal fun scaleCandidates(all: Int, view: IndexView, startUs: Long, endUs: Long, durationUs: Long): Int {
    if (view.complete || all <= 0 || view.lastPtsUs <= 0L || durationUs <= 0L) return all
    val full = minOf(durationUs, endUs) - startUs
    val covered = minOf(view.lastPtsUs, endUs) - startUs
    if (covered <= 0L || full <= covered) return all
    return (all.toDouble() * full / covered).toInt().coerceAtLeast(all)
}
