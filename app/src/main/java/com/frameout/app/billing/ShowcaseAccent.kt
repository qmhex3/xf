package com.frameout.app.billing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.pro.ProShowcase
import com.frameout.core.util.SettingStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameAccent
import com.frameout.designsystem.theme.FrameAccents
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private const val ORB_STAGGER_MS = 80L
private const val ARC_DP = 3.4f
private val Orbs = FrameAccents.All.take(6)

internal fun accentBrush(accent: FrameAccent): Brush = Brush.linearGradient(listOf(accent.c, accent.a, accent.b))

internal fun arcOffsetDp(index: Int, count: Int): Float {
    val centered = index - (count - 1) / 2f
    return centered * centered * ARC_DP
}

@Composable
internal fun AccentShowcase(selected: FrameAccent, onPick: (FrameAccent) -> Unit) {
    val colors = FrameTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 22.dp)) {
            Orbs.forEachIndexed { index, accent -> Orb(accent, index, accent.key == selected.key, onPick) }
        }
        FrameText(SettingStrings.accentLabel(selected.key), FrameTheme.typography.titleSmall, colors.onSurface)
        FrameText(ProShowcase.tryHint(), FrameTheme.typography.bodySmall, colors.onSurfaceVariant, Modifier.padding(top = 2.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun Orb(accent: FrameAccent, index: Int, on: Boolean, onPick: (FrameAccent) -> Unit) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val appear = remember { Animatable(if (reduce) 1f else 0f) }
    val grow = remember { Animatable(if (on) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay(250L + ORB_STAGGER_MS * index)
        appear.animateTo(1f, spring(0.55f, Spring.StiffnessMediumLow))
    }
    LaunchedEffect(on) { grow.animateTo(if (on) 1f else 0f, if (reduce) spring(stiffness = Spring.StiffnessHigh) else spring(0.55f, Spring.StiffnessMedium)) }
    val label = SettingStrings.accentLabel(accent.key)
    Box(
        Modifier
            .offset(y = arcOffsetDp(index, Orbs.size).dp)
            .size(42.dp)
            .graphicsLayer {
                val s = appear.value * (1f + 0.28f * grow.value)
                scaleX = s
                scaleY = s
                alpha = appear.value.coerceIn(0f, 1f)
            }
            .background(accentBrush(accent), CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.28f + 0.72f * grow.value), CircleShape)
            .semantics { contentDescription = label }
            .frameClickable(onClickLabel = label) { onPick(accent) },
        Alignment.Center
    ) {
        if (on) Image(painterResource(FrameIcons.Check), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(Color.White))
    }
}
