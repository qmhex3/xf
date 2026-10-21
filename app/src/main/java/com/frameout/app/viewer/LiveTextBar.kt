package com.frameout.app.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun LiveTextBar(
    state: LiveTextState,
    onToggle: () -> Unit,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FrameTheme.colors
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 60.dp)) {
        if (!state.active) return@Box
        Row(
            Modifier.align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                state.scanning -> ChromePill { FrameText(QualityStrings.liveTextReading(), style = FrameTheme.typography.label, color = colors.onSurfaceVariant) }
                state.failed -> ChromePill { FrameText(QualityStrings.liveTextFailed(), style = FrameTheme.typography.label, color = colors.error) }
                state.words.isEmpty() -> ChromePill { FrameText(QualityStrings.liveTextNone(), style = FrameTheme.typography.label, color = colors.onSurfaceVariant) }
                state.hasSelection -> {
                    ChromePill(Modifier.frameClickable(onClick = onCopy)) {
                        FrameText(QualityStrings.liveTextCopy(), style = FrameTheme.typography.label, color = colors.primary)
                    }
                    ChromePill(Modifier.alpha(0.85f).frameClickable(onClick = onSelectAll)) {
                        FrameText(QualityStrings.liveTextSelectAll(), style = FrameTheme.typography.label, color = colors.onSurface)
                    }
                }
                else -> {
                    ChromePill { FrameText(QualityStrings.liveTextHint(), style = FrameTheme.typography.label, color = colors.onSurfaceVariant) }
                    ChromePill(Modifier.frameClickable(onClick = onSelectAll)) {
                        FrameText(QualityStrings.liveTextSelectAll(), style = FrameTheme.typography.label, color = colors.onSurface)
                    }
                }
            }
            ChromePill(Modifier.frameClickable(onClick = onToggle)) {
                FrameText(com.frameout.core.util.QualityStrings.contentClose(), style = FrameTheme.typography.label, color = colors.primary)
            }
        }
    }
}
