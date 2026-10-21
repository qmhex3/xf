package com.frameout.app.frames

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.trim.ACTION_EXTRA_MS
import com.frameout.app.trim.CaptureStatusState
import com.frameout.app.trim.toIsland
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameIslandToast
import com.frameout.designsystem.components.IslandAction
import com.frameout.designsystem.components.IslandToastState
import com.frameout.designsystem.components.ToastKind
import com.frameout.designsystem.components.islandToastState
import com.frameout.designsystem.components.toastDurationMs
import com.frameout.designsystem.icons.FrameIconsExtra

@Composable
internal fun FramesToast(state: FramesState, vm: FramesViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val cancel = FrameStrings.cancel()
    val undoLabel = FrameStrings.undo()
    val progress = state.gallerySave
    val export = state.exportSaved
    val message = toastMessage(state)
    val busy = toastBusy(state)
    val undoable = state.undoGalleryIds.isNotEmpty()

    val island: IslandToastState? = remember(progress?.done, progress?.total, export, message, busy, state.messageSaved, state.messageIcon, undoable, cancel, undoLabel) {
        toastIsland(state, vm, context, cancel, undoLabel)
    }
    FrameIslandToast(island, modifier, toastDuration(state), toastDismiss(state, vm))
}

private fun toastMessage(state: FramesState): String? =
    state.message?.takeIf { state.undoDelete.isEmpty() && state.gallerySave == null }

private fun toastBusy(state: FramesState): Boolean = state.messageBusy || state.analyzing

private fun toastKind(message: String): ToastKind =
    if (isStatusError(message)) ToastKind.Error else ToastKind.Success

private fun toastIsland(
    state: FramesState,
    vm: FramesViewModel,
    context: Context,
    cancel: String,
    undoLabel: String
): IslandToastState? {
    val progress = state.gallerySave
    val export = state.exportSaved
    val message = toastMessage(state)
    return when {
        progress != null -> islandToastState(
            FrameStrings.gallerySavingProgress(progress.done, progress.total),
            busy = true,
            actions = listOf(IslandAction(cancel, FrameIconsExtra.Cancel, onClick = vm::cancelGallerySave))
        )
        export != null -> CaptureStatusState(FrameStrings.savedOk(), export).toIsland(context)
        message != null -> messageIsland(state, vm, message, undoLabel)
        else -> null
    }
}

private fun messageIsland(state: FramesState, vm: FramesViewModel, message: String, undoLabel: String): IslandToastState {
    val undoable = state.undoGalleryIds.isNotEmpty()
    return islandToastState(
        message,
        kind = toastKind(message),
        busy = toastBusy(state),
        actions = if (undoable) listOf(IslandAction(undoLabel, FrameIconsExtra.Undo, primary = true, onClick = vm::undoGallerySave)) else emptyList(),
        icon = state.messageSaved?.icon ?: state.messageIcon,
        saved = state.messageSaved != null
    )
}

private fun toastDuration(state: FramesState): Long? {
    val export = state.exportSaved
    val message = toastMessage(state)
    return when {
        state.gallerySave != null || toastBusy(state) && export == null -> null
        export != null -> toastDurationMs(FrameStrings.savedOk(), ToastKind.Success) + ACTION_EXTRA_MS
        message != null ->
            if (state.undoGalleryIds.isNotEmpty()) GALLERY_UNDO_WINDOW_MS else toastDurationMs(message, toastKind(message))
        else -> null
    }
}

private fun toastDismiss(state: FramesState, vm: FramesViewModel): (() -> Unit)? = when {
    state.gallerySave != null -> null
    state.exportSaved != null -> vm::clearExport
    else -> vm::clearMessage
}
