package com.frameout.app.trim

import com.frameout.designsystem.theme.frameControl
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.components.TopBarIconButton
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.engine.player.PlayerController
import com.frameout.engine.timeline.formatTimestamp

@Composable
internal fun TrimHeader(
    controller: PlayerController, durationMs: Long, outputIcon: Int, outputChanged: Boolean, showOutput: Boolean,
    onBack: () -> Unit, onOutput: () -> Unit, onTools: () -> Unit
) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
        Arrangement.spacedBy(8.dp),
        Alignment.CenterVertically
    ) {
        TopBarIconButton(FrameIcons.Back, FrameStrings.back(), onBack, Modifier.frameControl(CircleShape))
        Box(Modifier.weight(1f), Alignment.Center) { Timecode(controller, durationMs, Modifier) }
        if (showOutput) Box(Modifier.size(48.dp)) {
            TopBarIconButton(
                FrameIconsExtra.OutputPicker, TrimStrings.outputTitle(), onOutput,
                Modifier.frameControl(CircleShape, selected = outputChanged, glowAlpha = if (outputChanged) 0.8f else 0f)
            )
            if (outputChanged) Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 3.dp, y = 3.dp).size(20.dp)
                    .clip(CircleShape).background(colors.primary).border(1.5.dp, colors.background, CircleShape),
                Alignment.Center
            ) {
                Image(painterResource(outputIcon), null, Modifier.size(12.dp), colorFilter = ColorFilter.tint(colors.onPrimary))
            }
        }
        TopBarIconButton(FrameIcons.Tools, TrimStrings.toolsTitle(), onTools, Modifier.frameControl(CircleShape))
    }
}

@Composable
private fun Timecode(controller: PlayerController, durationMs: Long, modifier: Modifier) {
    val position by controller.position.collectAsState()
    val colors = FrameTheme.colors
    val mono = FrameTheme.typography.mono.copy(textDirection = TextDirection.Ltr)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier.frameControl(FrameShapes.pill, selected = true, glowAlpha = 0.4f).padding(horizontal = 12.dp, vertical = 9.dp),
            Arrangement.spacedBy(8.dp),
            Alignment.CenterVertically
        ) {
            FrameText(formatTimestamp(position), style = mono, color = colors.onSurface)
            FrameText("/", style = mono, color = colors.onSurfaceVariant)
            FrameText(formatTimestamp(durationMs), style = mono, color = colors.onSurfaceVariant)
        }
    }
}
