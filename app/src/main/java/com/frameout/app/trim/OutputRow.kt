package com.frameout.app.trim

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.app.settings.RadioMark
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline
import com.frameout.engine.timeline.formatTimestamp

internal fun outputKind(output: TrimOutput): String = when (output) {
    TrimOutput.Frames -> "frames"
    TrimOutput.Video -> "video"
    TrimOutput.Live -> "live"
    TrimOutput.Gif -> "gif"
    TrimOutput.Webp -> "webp"
}

@Composable
internal fun OutputBadge(output: TrimOutput, selected: Boolean) {
    val colors = FrameTheme.colors
    Box(
        Modifier.size(40.dp).clip(FrameShapes.md).background(if (selected) colors.primary.copy(alpha = 0.16f) else colors.containerHigh),
        Alignment.Center
    ) {
        Image(painterResource(outputIcon(output)), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(if (selected) colors.primary else colors.onSurfaceVariant))
    }
}

@Composable
internal fun BoxScope.OutputSheet(env: TrimEnv, visible: Boolean) {
    TrimSheetHost(visible, TrimStrings.outputTitle(), { env.onSheet(TrimSheet.None) }) {
        TrimOutput.entries.forEach { output ->
            OutputOption(output, env.output == output) {
                env.onOutput(output)
                env.onSheet(TrimSheet.None)
            }
        }
    }
}

@Composable
private fun OutputOption(output: TrimOutput, selected: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FrameShapes.md)
            .background(if (selected) colors.primary.copy(alpha = 0.12f) else colors.fieldFill)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) colors.primary else frameHairline(), FrameShapes.md)
            .frameSelectable(selected, Role.RadioButton, onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        Arrangement.spacedBy(12.dp),
        Alignment.CenterVertically
    ) {
        OutputBadge(output, selected)
        Column(Modifier.weight(1f), Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FrameText(
                    outputLabel(output),
                    style = FrameTheme.typography.titleSmall,
                    color = if (selected) colors.primary else colors.onSurface
                )
                if (output.limited) {
                    FrameText(
                        TrimStrings.upTo(output.seconds),
                        style = FrameTheme.typography.monoSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.clip(FrameShapes.pill).background(colors.containerHigh).padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            FrameText(TrimStrings.outDesc(outputKind(output)), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        RadioMark(selected)
    }
}

@Composable
internal fun OutputHint(env: TrimEnv) {
    if (!env.output.limited) return
    FrameText(TrimStrings.limitHint(env.output.seconds), style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
}
