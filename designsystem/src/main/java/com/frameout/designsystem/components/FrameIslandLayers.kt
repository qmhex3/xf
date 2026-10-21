package com.frameout.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.liveMessage
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.min

private val INK = Color(0xFFF4F6F5)
private val INK_DIM = Color.White.copy(alpha = 0.62f)
private val ON_ACCENT = Color(0xFF0B0B0B)

@Composable
internal fun CompactLayer(
    s: IslandToastState,
    accent: Color,
    checkP: () -> Float,
    pop: () -> Float,
    liveText: Boolean
) {
    Row(
        Modifier.height(COMPACT_H).padding(start = 5.dp, end = 14.dp),
        Arrangement.spacedBy(8.dp),
        Alignment.CenterVertically
    ) {
        IslandBadge(s, accent, 24.dp, checkP, pop)
        FrameText(
            s.title,
            style = FrameTheme.typography.label.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
            color = if (s.kind == ToastKind.Error) accent else INK,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (liveText) Modifier.liveMessage() else Modifier
        )
    }
}

@Composable
internal fun ExpandedLayer(
    s: IslandToastState,
    accent: Color,
    checkP: () -> Float,
    pop: () -> Float
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        Arrangement.spacedBy(12.dp),
        Alignment.CenterVertically
    ) {
        IslandBadge(s, accent, 32.dp, checkP, pop)
        Column(Modifier.weight(1f), Arrangement.spacedBy(1.dp)) {
            FrameText(
                s.title,
                style = FrameTheme.typography.label.copy(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
                color = if (s.kind == ToastKind.Error) accent else INK,
                maxLines = if (s.detail == null) 3 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.liveMessage()
            )
            if (s.detail != null) {
                FrameText(
                    s.detail,
                    style = FrameTheme.typography.monoSmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
                    color = INK_DIM,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        s.actions.forEach { IslandActionButton(it, accent) }
    }
}

@Composable
private fun IslandBadge(
    s: IslandToastState,
    accent: Color,
    size: Dp,
    checkP: () -> Float,
    pop: () -> Float
) {
    val busyA by animateFloatAsState(if (s.busy) 1f else 0f, tween(160), label = "islandBusy")
    Box(
        Modifier.size(size).graphicsLayer { val k = pop(); scaleX = k; scaleY = k },
        Alignment.Center
    ) {
        if (busyA > 0.01f) {
            FrameSpinner(Modifier.graphicsLayer { alpha = busyA }, size * 0.78f, accent, 2.dp)
        }
        if (busyA < 0.99f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - busyA }, Alignment.Center) {
                BadgeFace(s, accent, size, checkP)
            }
        }
    }
}

@Composable
private fun BadgeFace(s: IslandToastState, accent: Color, size: Dp, checkP: () -> Float) {
    val icon = s.icon
    when {
        icon != null && s.saved && s.kind == ToastKind.Success -> SavedFace(icon, accent, size, checkP)
        icon != null && s.kind != ToastKind.Error -> TonalFace(icon, accent, size)
        s.kind == ToastKind.Success -> Canvas(Modifier.fillMaxSize()) { drawDone(accent, checkP()) }
        else -> TonalFace(if (s.kind == ToastKind.Error) FrameIconsExtra.Warning else FrameIcons.Info, accent, size)
    }
}

@Composable
private fun TonalFace(icon: Int, accent: Color, size: Dp) {
    Box(Modifier.fillMaxSize().clip(CircleShape).background(accent.copy(alpha = 0.20f)))
    Image(
        painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(size * 0.62f),
        colorFilter = ColorFilter.tint(accent)
    )
}

@Composable
private fun SavedFace(icon: Int, accent: Color, size: Dp, checkP: () -> Float) {
    Box(Modifier.fillMaxSize().clip(CircleShape).background(accent))
    Image(
        painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(size * 0.6f),
        colorFilter = ColorFilter.tint(ON_ACCENT)
    )
    Canvas(Modifier.fillMaxSize()) { drawSavedChip(accent, checkP()) }
}

private fun DrawScope.drawSavedChip(accent: Color, p: Float) {
    if (p <= 0f) return
    val r = size.minDimension * 0.2f * p
    val c = Offset(size.width * 0.84f, size.height * 0.84f)
    drawCircle(Color.Black, r + size.minDimension * 0.05f * p, c)
    drawCircle(accent, r, c)
    val w = size.minDimension * 0.045f
    val a = Offset(c.x - r * 0.45f, c.y + r * 0.02f)
    val b = Offset(c.x - r * 0.1f, c.y + r * 0.38f)
    val d = Offset(c.x + r * 0.5f, c.y - r * 0.38f)
    drawLine(ON_ACCENT, a, b, w, StrokeCap.Round)
    drawLine(ON_ACCENT, b, d, w, StrokeCap.Round)
}

private fun DrawScope.drawDone(accent: Color, p: Float) {
    drawCircle(accent)
    if (p < 1f) {
        drawCircle(
            accent.copy(alpha = 0.45f * (1f - p)),
            radius = size.minDimension * (0.5f + 0.55f * p),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
    val a = Offset(size.width * 0.28f, size.height * 0.52f)
    val b = Offset(size.width * 0.44f, size.height * 0.68f)
    val c = Offset(size.width * 0.73f, size.height * 0.35f)
    val first = (b - a).getDistance()
    val second = (c - b).getDistance()
    val drawn = (first + second) * p
    val w = size.minDimension * 0.10f
    drawLine(ON_ACCENT, a, a + (b - a) * (min(drawn, first) / first), w, StrokeCap.Round)
    if (drawn > first) drawLine(ON_ACCENT, b, b + (c - b) * ((drawn - first) / second), w, StrokeCap.Round)
}

@Composable
private fun IslandActionButton(action: IslandAction, accent: Color) {
    val tint = if (action.primary) accent else Color.White
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (action.primary) accent.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f))
            .border(1.dp, if (action.primary) accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f), CircleShape)
            .semantics { contentDescription = action.label }
            .frameClickable(onClickLabel = action.label, onClick = action.onClick),
        Alignment.Center
    ) {
        Image(
            painterResource(action.icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}
