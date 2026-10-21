package com.frameout.designsystem.dock

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun FrameStatusStrip(
    title: String,
    detail: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionColor: Color = FrameTheme.colors.onSurfaceVariant,
    progress: Float? = null,
    indeterminate: Boolean = false
) {
    val colors = FrameTheme.colors
    GlassSurface(shape = FrameShapes.pill, glow = colors.primary, modifier = modifier.widthIn(max = 380.dp)) {
        Box {
            Row(Modifier.padding(start = 20.dp, end = 4.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                FrameText(
                    title,
                    style = FrameTheme.typography.label,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (detail.isNotEmpty()) {
                    FrameText(detail, style = FrameTheme.typography.monoSmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp), maxLines = 1)
                }
                FrameText(
                    actionLabel,
                    style = FrameTheme.typography.label,
                    color = actionColor,
                    modifier = Modifier.padding(start = 6.dp).frameClickable(onClick = onAction).padding(horizontal = 14.dp),
                    maxLines = 1
                )
            }
            StripProgress(progress, indeterminate, Modifier.align(Alignment.BottomStart))
        }
    }
}

@Composable
private fun StripProgress(progress: Float?, indeterminate: Boolean, modifier: Modifier) {
    if (progress == null && !indeterminate) return
    val colors = FrameTheme.colors
    val track = modifier.fillMaxWidth().height(4.dp).background(colors.onSurface.copy(alpha = 0.12f))
    if (progress != null) {
        val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(350), label = "stripProgress")
        Box(track) { Box(Modifier.fillMaxWidth(animated.coerceAtLeast(0.02f)).height(4.dp).background(FrameGradients.Progress)) }
    } else {
        Box(track.then(stripSweep()))
    }
}

@Composable
private fun stripSweep(): Modifier {
    val reduce = LocalSystemA11y.current.reduceMotion
    val x = if (reduce) null else rememberInfiniteTransition(label = "stripSweep").animateFloat(
        initialValue = -0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "stripSweepX"
    )
    val sweep = FrameGradients.Progress
    return Modifier.drawBehind {
        val start = (x?.value ?: 0.3f) * size.width
        drawRect(sweep, Offset(start, 0f), Size(size.width * 0.4f, size.height))
    }
}
