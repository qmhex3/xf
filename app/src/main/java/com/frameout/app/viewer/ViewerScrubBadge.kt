package com.frameout.app.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.frameout.core.util.SavedStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun ViewerScrubBadge(multiple: Int, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = multiple > 1,
        modifier = modifier,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        val label = SavedStrings.scrubSpeed(multiple).latinDigits()
        ChromePill(Modifier.semantics { contentDescription = label }) {
            FrameText("×$multiple", style = FrameTheme.typography.mono, color = FrameTheme.colors.primary)
        }
    }
}
