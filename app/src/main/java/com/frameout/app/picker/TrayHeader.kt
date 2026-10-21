package com.frameout.app.picker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.PickerStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.liveMessage
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun TrayHeader(count: Int, showHint: Boolean) {
    val colors = FrameTheme.colors
    Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        FrameText(
            FrameStrings.selectedCount(count),
            style = FrameTheme.typography.label,
            color = colors.onSurface,
            maxLines = 1,
            modifier = Modifier.liveMessage()
        )
        if (showHint) {
            FrameText(
                PickerStrings.reorderHint(),
                style = FrameTheme.typography.monoSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
internal fun ContinueButton(count: Int, onClick: () -> Unit) {
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val description = PickerStrings.continueDescription(count)
    Box(
        Modifier
            .frameClickable(onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description }
            .background(FrameGradients.Brand, FrameShapes.pill)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    val up = targetState > initialState
                    (slideInVertically(tween(220)) { if (up) it / 2 else -it / 2 } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(tween(220)) { if (up) -it / 2 else it / 2 } + fadeOut(tween(120)))
                }
            },
            label = "trayCount"
        ) { value ->
            FrameText(FrameStrings.continueN(value), style = FrameTheme.typography.titleSmall, color = Color.White)
        }
    }
}
