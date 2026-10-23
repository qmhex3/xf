package com.frameout.app.trim

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardEdge
import com.frameout.designsystem.theme.frameHairline

@Composable
internal fun PanelActions(env: TrimEnv) {
    Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
            if (env.output == TrimOutput.Frames) PlanButton(env, Modifier.weight(1f))
            TrimButton(primaryLabel(env), primaryIcon(env), Modifier.weight(1f), primary = true, enabled = !env.busy, onClick = env.onPrimary)
        }
        if (env.output == TrimOutput.Frames) PlanWarning(env)
    }
}

private fun primaryLabel(env: TrimEnv): String = when {
    env.batchEdit -> TrimStrings.batchApply()
    env.output == TrimOutput.Frames -> TrimStrings.extractFrames()
    else -> TrimStrings.saveAs(outputLabel(env.output))
}

private fun primaryIcon(env: TrimEnv): Int = if (env.batchEdit) FrameIcons.Check else outputIcon(env.output)

internal fun outputLabel(output: TrimOutput): String = when (output) {
    TrimOutput.Frames -> TrimStrings.outFrames()
    TrimOutput.Video -> TrimStrings.outVideo()
    TrimOutput.Live -> TrimStrings.outLive()
    TrimOutput.Gif -> TrimStrings.outGif()
    TrimOutput.Webp -> TrimStrings.outWebp()
}

internal fun outputIcon(output: TrimOutput): Int = when (output) {
    TrimOutput.Frames -> FrameIconsExtra.Frames
    TrimOutput.Video -> FrameIconsExtra.Clip
    TrimOutput.Live -> FrameIconsExtra.Live
    TrimOutput.Gif -> FrameIconsExtra.Gif
    TrimOutput.Webp -> FrameIconsExtra.Webp
}

@Composable
internal fun TrimButton(label: String, icon: Int, modifier: Modifier, primary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val fill: Brush = if (primary) FrameGradients.Brand else Brush.linearGradient(listOf(colors.container, colors.container))
    val tint = if (primary) Color.White else colors.onSurface
    val click = if (enabled) Modifier.frameClickable(onClick = onClick) else Modifier
    val outline = if (primary) Modifier.border(1.dp, frameCardEdge(colors.accentA, 0.5f), FrameShapes.lg) else Modifier.border(1.dp, frameHairline(), FrameShapes.lg)
    val glow = if (primary) Modifier.frameGlow(colors.accentA, { if (colors.isDark) 0.45f else 0.2f }, 8.dp, { 0.43f }) else Modifier
    Row(
        modifier.then(click).height(56.dp).alpha(if (enabled) 1f else 0.5f).then(glow).clip(FrameShapes.lg).background(fill).then(outline).padding(horizontal = 12.dp),
        Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        Alignment.CenterVertically
    ) {
        Image(painterResource(icon), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(tint))
        FrameText(label, style = FrameTheme.typography.titleSmall, color = tint, maxLines = 1)
    }
}
