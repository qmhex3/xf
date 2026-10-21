package com.frameout.app.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.liveMessage
import com.frameout.designsystem.components.FrameSpinner
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.frameEnter
import com.frameout.designsystem.motion.frameExit
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun ViewerQualityBadge(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, enter = frameEnter(), exit = frameExit(), modifier = modifier) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 72.dp)
                .clip(FrameShapes.pill)
                .background(Color.Black.copy(alpha = 0.60f))
                .padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp)
                .liveMessage(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrameSpinner(size = 14.dp, color = Color.White, strokeWidth = 2.dp)
            FrameText(
                text = FrameStrings.loadingFullQuality(),
                style = FrameTheme.typography.label,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
