package com.frameout.app.frames

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameToastHost
import com.frameout.designsystem.components.ToastAction
import com.frameout.designsystem.components.ToastKind
import com.frameout.designsystem.icons.FrameIcons

@Composable
internal fun FramesUndoBar(text: String?, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    FrameToastHost(
        message = text,
        modifier = modifier,
        kind = ToastKind.Success,
        icon = FrameIcons.Trash,
        durationMs = UNDO_MS,
        actions = listOf(ToastAction(FrameStrings.undo(), onUndo))
    )
}

private const val UNDO_MS = 6000L
