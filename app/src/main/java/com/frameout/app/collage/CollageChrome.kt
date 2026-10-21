package com.frameout.app.collage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.util.CollageStrings
import com.frameout.designsystem.a11y.liveMessage
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun PreviewHint(state: CollageState) {
    val colors = FrameTheme.colors
    val overflow = state.ready && state.frames.size > state.template.cells.size
    val text = when {
        !state.ready -> CollageStrings.preparing()
        overflow -> CollageStrings.overflow(state.template.cells.size, state.frames.size)
        state.selectedCell != null -> CollageStrings.hintSelected()
        state.frames.size > 1 -> CollageStrings.hintSwap()
        state.frames.size == 1 -> CollageStrings.hintAdjust()
        else -> return
    }
    Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 24.dp, vertical = 4.dp), Alignment.Center) {
        FrameText(
            text,
            style = FrameTheme.typography.bodySmall,
            color = if (overflow) colors.primary else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.liveMessage()
        )
    }
}

@Composable
fun ToastHost(message: String?, isError: Boolean, modifier: Modifier = Modifier) {
    com.frameout.designsystem.components.FrameToastHost(
        message,
        modifier,
        if (isError) com.frameout.designsystem.components.ToastKind.Error else com.frameout.designsystem.components.ToastKind.Success
    )
}
