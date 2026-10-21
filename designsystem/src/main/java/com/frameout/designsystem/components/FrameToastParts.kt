package com.frameout.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun ToastIcon(icon: Int, accent: Color, busy: Boolean, drawnCheck: Boolean = false) {
    if (busy) {
        FrameSpinner(size = 20.dp, color = accent, strokeWidth = 2.dp)
    } else if (drawnCheck) {
        FrameCheck(accent)
    } else {
        Image(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(accent)
        )
    }
}

@Composable
internal fun ToastBadge(icon: Int, accent: Color, busy: Boolean, drawnCheck: Boolean) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        ToastIcon(icon, accent, busy, drawnCheck)
    }
}

@Composable
internal fun ToastActions(actions: List<ToastAction>, accent: Color) {
    Row(Modifier.fillMaxWidth().padding(start = 48.dp), Arrangement.spacedBy(8.dp)) {
        actions.forEach { ToastActionButton(it, accent, Modifier.weight(1f, fill = actions.size > 1)) }
    }
}

@Composable
internal fun ToastActionButton(action: ToastAction, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(FrameShapes.pill)
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.32f), FrameShapes.pill)
            .frameClickable(onClickLabel = action.label, onClick = action.onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        FrameText(
            action.label,
            style = FrameTheme.typography.label.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = accent,
            maxLines = 1
        )
    }
}

@Composable
internal fun ToastDismiss(onDismiss: () -> Unit, label: String?) {
    Box(
        Modifier
            .clip(CircleShape)
            .frameClickable(onClickLabel = label, onClick = onDismiss)
            .semantics { if (label != null) contentDescription = label }
            .size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(FrameIcons.Close),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            colorFilter = ColorFilter.tint(FrameTheme.colors.onSurfaceVariant)
        )
    }
}

@Composable
internal fun ToastTimer(key: Any, durationMs: Long, accent: Color, modifier: Modifier = Modifier) {
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val progress = remember(key, durationMs) { Animatable(1f) }
    LaunchedEffect(key, durationMs) {
        progress.snapTo(1f)
        progress.animateTo(0f, tween(durationMs.toInt(), easing = LinearEasing))
    }
    if (reduceMotion) return
    Box(
        modifier
            .fillMaxWidth()
            .height(2.dp)
            .layout { measurable, constraints ->
                val width = (constraints.maxWidth * progress.value).toInt()
                val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                layout(constraints.maxWidth, placeable.height) { placeable.place(0, 0) }
            }
            .background(accent.copy(alpha = 0.7f))
    )
}
