package com.frameout.app.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.frameout.app.billing.ProBadge
import com.frameout.app.billing.rememberProGate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ModeStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline
import com.frameout.engine.extract.ExtractModeCodec

private const val CELLS = 12

@Composable
internal fun ExtractModePicker(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, Arrangement.spacedBy(10.dp)) {
        ModeKinds.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), Arrangement.spacedBy(10.dp)) {
                row.forEach { kind ->
                    ModeCard(kind, kind == selected, Modifier.weight(1f).fillMaxHeight()) { onSelect(kind) }
                }
            }
        }
        ModeExplainer(selected)
    }
}

@Composable
internal fun ModeSheet(visible: Boolean, prefs: AppPrefs, stored: String, onClose: () -> Unit) {
    val mode = ExtractModeCodec.decode(stored)
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.68f).dp
    BottomSheetFrame(visible, onClose, FrameStrings.defaultMode()) {
        Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) {
            ExtractModePicker(kindOf(mode), { kind -> gateMode(kind) { prefs.setExtractMode(ExtractModeCodec.encode(modeFor(kind, mode))) } })
        }
        Box(
            Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(FrameShapes.pill)
                .background(FrameGradients.Brand)
                .frameClickable(onClick = onClose),
            Alignment.Center
        ) {
            FrameText(FrameStrings.done(), style = FrameTheme.typography.label, color = Color.White)
        }
    }
}

@Composable
private fun ModeCard(kind: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val locked = rememberProGate().locked(kind != "scene" && kind != "slides")
    val edge by animateColorAsState(if (selected) colors.primary else frameHairline(), frameSpring(), label = "modeEdge")
    Column(
        modifier
            .clip(FrameShapes.md)
            .background(if (selected) colors.primary.copy(alpha = 0.12f) else colors.fieldFill)
            .border(if (selected) 1.5.dp else 1.dp, edge, FrameShapes.md)
            .frameSelectable(selected, Role.RadioButton, onClick)
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
        ModeIllustration(kind, selected, Modifier.padding(horizontal = 4.dp))
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 10.dp, top = 2.dp, bottom = 8.dp),
            Arrangement.spacedBy(8.dp),
            Alignment.CenterVertically
        ) {
            FrameText(
                FrameStrings.settingValue(kind),
                style = FrameTheme.typography.body.copy(fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal),
                color = if (selected) colors.primary else colors.onSurface,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
            if (locked) ProBadge(compact = true)
            RadioMark(selected)
        }
    }
}

@Composable
private fun ModeExplainer(kind: String) {
    val colors = FrameTheme.colors
    Crossfade(kind, label = "modeHelp") { k ->
        Column(
            Modifier.fillMaxWidth().clip(FrameShapes.md).background(colors.fieldFill).padding(14.dp),
            Arrangement.spacedBy(4.dp)
        ) {
            FrameText(FrameStrings.settingValue(k), style = FrameTheme.typography.titleSmall, color = colors.onSurface)
            FrameText(ModeStrings.modeHelp(k), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ModeIllustration(kind: String, selected: Boolean, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val pick = colors.primary
    val dim = colors.onSurfaceVariant.copy(alpha = if (selected) 0.30f else 0.22f)
    val ink = colors.onSurfaceVariant.copy(alpha = 0.55f)
    val sceneB = colors.success
    val sceneC = colors.error
    Canvas(modifier.fillMaxWidth().height(60.dp)) {
        if (kind == "slides") drawSlides(pick, dim, ink) else drawStrip(kind, pick, dim, ink, pick, sceneB, sceneC)
    }
}

private fun DrawScope.drawStrip(kind: String, pick: Color, dim: Color, ink: Color, sceneA: Color, sceneB: Color, sceneC: Color) {
    val pad = 10.dp.toPx()
    val gap = 3.dp.toPx()
    val cw = (size.width - pad * 2 - gap * (CELLS - 1)) / CELLS
    val base = 24.dp.toPx()
    val lift = 4.dp.toPx()
    val top = (size.height - base) / 2f - 2.dp.toPx()
    val radius = CornerRadius(3.dp.toPx())
    val picks: Set<Int> = when (kind) {
        "every_frame" -> (0 until CELLS).toSet()
        "every_n" -> setOf(0, 3, 6, 9)
        "every_time" -> setOf(0, 4, 8)
        "keyframes" -> setOf(0, 5, 9)
        "fixed" -> setOf(1, 4, 7, 10)
        else -> setOf(0, 4, 8)
    }
    fun x(i: Int) = pad + i * (cw + gap)
    for (i in 0 until CELLS) {
        val picked = i in picks
        val color = when {
            kind == "scene" -> (when (i / 4) { 0 -> sceneA; 1 -> sceneB; else -> sceneC }).copy(alpha = if (picked) 1f else 0.28f)
            picked -> pick
            else -> dim
        }
        val y = if (picked) top - lift else top
        val h = if (picked) base + lift * 2 else base
        drawRoundRect(color, Offset(x(i), y), Size(cw, h), radius)
    }
    val bottom = top + base + lift
    when (kind) {
        "every_time" -> {
            val ry = bottom + 8.dp.toPx()
            drawLine(ink, Offset(pad, ry), Offset(size.width - pad, ry), 1.5.dp.toPx())
            for (i in 0 until CELLS) {
                val cx = x(i) + cw / 2f
                val len = if (i in picks) 7.dp.toPx() else 3.dp.toPx()
                drawLine(if (i in picks) pick else ink, Offset(cx, ry), Offset(cx, ry - len), 1.5.dp.toPx())
            }
        }
        "keyframes" -> picks.forEach { i ->
            val cx = x(i) + cw / 2f
            val cy = top - lift - 7.dp.toPx()
            val r = 4.dp.toPx()
            val diamond = Path().apply {
                moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close()
            }
            drawPath(diamond, pick)
        }
        "fixed" -> {
            val by = bottom + 6.dp.toPx()
            val cap = 4.dp.toPx()
            val stroke = 1.5.dp.toPx()
            drawLine(ink, Offset(pad, by), Offset(size.width - pad, by), stroke)
            drawLine(ink, Offset(pad, by), Offset(pad, by - cap), stroke)
            drawLine(ink, Offset(size.width - pad, by), Offset(size.width - pad, by - cap), stroke)
            picks.forEach { i -> drawCircle(pick, 2.5.dp.toPx(), Offset(x(i) + cw / 2f, by)) }
        }
        "scene" -> listOf(4, 8).forEach { i ->
            val cx = x(i) - gap / 2f
            drawLine(
                ink, Offset(cx, top - lift - 4.dp.toPx()), Offset(cx, bottom + 4.dp.toPx()), 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
            )
        }
    }
}

private fun DrawScope.drawSlides(pick: Color, dim: Color, ink: Color) {
    val gap = 8.dp.toPx()
    val w = minOf((size.width - 20.dp.toPx() - gap * 2) / 3f, 84.dp.toPx())
    val h = 38.dp.toPx()
    val startX = (size.width - (w * 3 + gap * 2)) / 2f
    val y = (size.height - h) / 2f
    val pad = 6.dp.toPx()
    for (i in 0..2) {
        val x = startX + i * (w + gap)
        drawRoundRect(dim.copy(alpha = 0.18f), Offset(x, y), Size(w, h), CornerRadius(5.dp.toPx()))
        drawRoundRect(pick, Offset(x, y), Size(w, h), CornerRadius(5.dp.toPx()), style = Stroke(1.5.dp.toPx()))
        drawRoundRect(pick, Offset(x + pad, y + 7.dp.toPx()), Size(w * 0.5f, 4.dp.toPx()), CornerRadius(2.dp.toPx()))
        drawRoundRect(ink.copy(alpha = 0.5f), Offset(x + pad, y + 16.dp.toPx()), Size(w - pad * 2, 3.dp.toPx()), CornerRadius(1.5.dp.toPx()))
        drawRoundRect(ink.copy(alpha = 0.5f), Offset(x + pad, y + 23.dp.toPx()), Size((w - pad * 2) * 0.7f, 3.dp.toPx()), CornerRadius(1.5.dp.toPx()))
    }
}
