package com.frameout.app.trim

import androidx.compose.ui.graphics.Brush
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.theme.frameControl
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameHoldAccelerate
import com.frameout.designsystem.a11y.frameHoldRepeat
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill

@Composable
internal fun TrimChip(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    icon: Int? = null,
    onClick: () -> Unit
) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.pill
    val click = if (enabled) Modifier.frameClickable(onClick = onClick) else Modifier
    Row(
        modifier
            .then(click)
            .height(40.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .frameControl(shape, selected = selected, glowAlpha = if (selected) 0.8f else 0f)
            .clip(shape)
            .semantics { contentDescription = description }
            .padding(horizontal = 14.dp),
        Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        Alignment.CenterVertically
    ) {
        icon?.let { Image(painterResource(it), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.primary)) }
        if (label.isNotEmpty()) FrameText(label, style = FrameTheme.typography.label.copy(textDirection = TextDirection.Content), color = colors.onSurface)
    }
}

@Composable
internal fun TransportKey(description: String, modifier: Modifier = Modifier, onStep: (Int) -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier
            .frameHoldAccelerate(onStep)
            .size(48.dp)
            .frameControl(CircleShape)
            .clip(CircleShape)
            .semantics { contentDescription = description },
        Alignment.Center
    ) { content() }
}

@Composable
internal fun TransportIcon(icon: Int) {
    Image(painterResource(icon), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(FrameTheme.colors.primary))
}

@Composable
internal fun PlayButton(playing: Boolean, playLabel: String, pauseLabel: String, onToggle: () -> Unit) {
    Box(
        Modifier
            .frameClickable(onClick = onToggle)
            .size(64.dp)
            .frameGlow(FrameTheme.colors.primary, { 0.34f }, 18.dp)
            .clip(CircleShape)
            .background(FrameGradients.Fab)
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f))), CircleShape),
        Alignment.Center
    ) {
        Image(
            painterResource(if (playing) FrameIcons.PauseFilled else FrameIcons.PlayFilled),
            if (playing) pauseLabel else playLabel,
            Modifier.size(30.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
    }
}
