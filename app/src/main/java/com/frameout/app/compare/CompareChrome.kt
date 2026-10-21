package com.frameout.app.compare

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.frameout.designsystem.theme.frameHairline
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill

@Composable
internal fun PaneTag(item: PaneItem, modifier: Modifier) {
    val colors = FrameTheme.colors
    val badge = when (item.tag) { "A" -> colors.primary; "B" -> colors.success; else -> colors.error }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier.clip(FrameShapes.sm).background(Color.Black.copy(alpha = 0.62f)).padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(badge), Alignment.Center) {
                FrameText(item.tag, style = FrameTheme.typography.label.copy(fontSize = 11.sp), color = Color.Black)
            }
            Column {
                FrameText(item.title, style = FrameTheme.typography.monoSmall, color = Color.White, maxLines = 1)
                FrameText(item.subtitle, style = FrameTheme.typography.monoSmall.copy(fontSize = 9.sp), color = Color.White.copy(alpha = 0.78f), maxLines = 1)
            }
        }
    }
}

@Composable
internal fun Chip(content: @Composable BoxScope.() -> Unit) {
    GlassSurface(shape = FrameShapes.pill) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), content = content)
    }
}

@Composable
internal fun ModeSwitch(mode: CompareMode, onMode: (CompareMode) -> Unit) {
    GlassSurface(shape = FrameShapes.pill) {
        Row(Modifier.padding(4.dp).animateContentSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            CompareMode.entries.forEach { m ->
                val selected = m == mode
                val colors = FrameTheme.colors
                val tint = if (selected) colors.primary else colors.onSurfaceVariant
                val label = when (m) {
                    CompareMode.Stack -> CompareStrings.stack()
                    CompareMode.Side -> CompareStrings.side()
                    CompareMode.Slider -> CompareStrings.slider()
                    CompareMode.Diff -> CompareStrings.diff()
                }
                Row(
                    Modifier
                        .clip(FrameShapes.pill)
                        .then(if (selected) Modifier.background(colors.primary.copy(alpha = 0.18f)) else Modifier)
                        .frameClickable(role = Role.Tab, onClickLabel = label) { onMode(m) }
                        .semantics { contentDescription = label }
                        .padding(horizontal = if (selected) 14.dp else 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModeGlyph(m, tint)
                    if (selected) FrameText(label, style = FrameTheme.typography.label, color = tint, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ModeGlyph(mode: CompareMode, tint: Color) {
    Canvas(Modifier.size(16.dp)) {
        val s = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val r = CornerRadius(2.5.dp.toPx())
        val w = size.width
        val h = size.height
        when (mode) {
            CompareMode.Stack -> {
                drawRoundRect(tint, Offset(0f, 0f), Size(w, h * 0.44f), r, s)
                drawRoundRect(tint, Offset(0f, h * 0.56f), Size(w, h * 0.44f), r, s)
            }
            CompareMode.Side -> {
                drawRoundRect(tint, Offset(0f, 0f), Size(w * 0.44f, h), r, s)
                drawRoundRect(tint, Offset(w * 0.56f, 0f), Size(w * 0.44f, h), r, s)
            }
            CompareMode.Slider -> {
                drawRoundRect(tint, Offset(0f, 0f), Size(w, h), r, s)
                drawLine(tint, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), 1.6.dp.toPx())
            }
            CompareMode.Diff -> {
                val p = Path().apply { moveTo(w * 0.5f, h * 0.08f); lineTo(w * 0.94f, h * 0.9f); lineTo(w * 0.06f, h * 0.9f); close() }
                drawPath(p, tint, style = s)
            }
        }
    }
}

@Composable
internal fun ToolChip(icon: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val tint = if (selected) colors.primary else colors.onSurfaceVariant
    Row(
        Modifier
            .clip(FrameShapes.pill)
            .background(if (selected) colors.primary.copy(alpha = 0.16f) else colors.fieldFill)
            .border(1.dp, if (selected) colors.primary.copy(alpha = 0.5f) else frameHairline(), FrameShapes.pill)
            .frameClickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        Arrangement.spacedBy(6.dp),
        Alignment.CenterVertically
    ) {
        Image(painterResource(icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(tint))
        FrameText(label, style = FrameTheme.typography.label, color = tint, maxLines = 1)
    }
}
