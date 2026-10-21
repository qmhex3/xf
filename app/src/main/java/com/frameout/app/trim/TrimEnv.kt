package com.frameout.app.trim

import android.graphics.Bitmap
import android.net.Uri
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.storage.FrameCaptureSaver
import com.frameout.engine.player.PlayerController
import com.frameout.engine.player.PlayerUiState
import com.frameout.engine.timeline.TrimRange
import kotlinx.coroutines.CoroutineScope

internal enum class TrimSheet { None, Plan, Tools, Output }

internal class TrimEnv(
    val uri: Uri,
    val controller: PlayerController,
    val player: PlayerUiState,
    val prefs: AppPrefs,
    val queue: QueueState,
    val saver: FrameCaptureSaver,
    val scope: CoroutineScope,
    val markers: List<Long>,
    val range: TrimRange,
    val output: TrimOutput,
    val autoSave: Boolean,
    val busy: Boolean,
    val onRange: (TrimRange) -> Unit,
    val onOutput: (TrimOutput) -> Unit,
    val onSheet: (TrimSheet) -> Unit,
    val onAutoSave: () -> Unit,
    val onStatus: (CaptureStatusState?) -> Unit,
    val onPrimary: () -> Unit,
    val source: Uri = uri,
    val thumbs: List<Bitmap?> = emptyList(),
    val batchEdit: Boolean = false,
    val fx: SnapshotFxState = SnapshotFxState()
) {
    val clamped: TrimRange get() = range.clamped(player.durationMs)

    val effective: TrimRange
        get() = clamped.let { it.copy(useFull = it.startMs <= 0L && it.endMs >= player.durationMs) }
}
