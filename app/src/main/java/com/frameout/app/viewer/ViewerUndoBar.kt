package com.frameout.app.viewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SavedStrings
import com.frameout.designsystem.components.FrameToastHost
import com.frameout.designsystem.components.ToastAction
import com.frameout.designsystem.components.ToastKind
import com.frameout.designsystem.icons.FrameIcons

@Composable
internal fun ViewerUndoBar(visible: Boolean, savedImage: Boolean, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    FrameToastHost(
        message = (if (savedImage) SavedStrings.deleted() else FrameStrings.frameDeleted()).takeIf { visible },
        modifier = modifier,
        kind = ToastKind.Success,
        icon = FrameIcons.Trash,
        durationMs = UNDO_MS,
        actions = listOf(ToastAction(FrameStrings.undo(), onUndo))
    )
}

private const val UNDO_MS = 6000L
