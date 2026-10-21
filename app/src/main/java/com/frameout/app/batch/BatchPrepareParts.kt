package com.frameout.app.batch

import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import com.frameout.designsystem.icons.FrameIcons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import com.frameout.engine.timeline.formatTimestamp

internal val THUMB_HEIGHT = 84.dp

@Composable
internal fun RoundAction(icon: Int, description: String, primary: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (primary) colors.primary.copy(alpha = 0.16f) else colors.containerHigh)
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = description },
        Alignment.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(if (primary) colors.primary else colors.onSurfaceVariant))
    }
}

@Composable
internal fun Thumb(item: PrepItem) {
    val colors = FrameTheme.colors
    val bitmap = item.thumb
    Box(
        Modifier
            .size(width = 112.dp, height = THUMB_HEIGHT)
            .clip(FrameShapes.sm)
            .background(colors.containerHigh)
            .border(1.dp, frameHairline(), FrameShapes.sm),
        Alignment.Center
    ) {
        ThumbBody(item)
        if (bitmap != null) {
            Box(Modifier.fillMaxWidth().height(28.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))))
        }
        ThumbBadges(item)
    }
}

@Composable
private fun ThumbBody(item: PrepItem) {
    val colors = FrameTheme.colors
    val bitmap = item.thumb
    when {
        bitmap != null -> Image(remember(bitmap) { bitmap.asImageBitmap() }, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        !item.loaded -> {
            val pulse = rememberInfiniteTransition(label = "thumb")
            val a by pulse.animateFloat(0.10f, 0.32f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "thumbAlpha")
            Box(Modifier.fillMaxSize().background(colors.primary.copy(alpha = a)))
        }
        else -> Box(
            Modifier.fillMaxSize().background(Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.28f), colors.containerHigh))),
            Alignment.Center
        ) {
            Image(painterResource(FrameIconsExtra.Clip), null, Modifier.size(28.dp), colorFilter = ColorFilter.tint(colors.primary.copy(alpha = 0.85f)))
        }
    }
}

@Composable
private fun BoxScope.ThumbBadges(item: PrepItem) {
    if (item.edited) {
        Box(
            Modifier.align(Alignment.TopStart).padding(5.dp).size(22.dp).background(FrameTheme.colors.primary, CircleShape),
            Alignment.Center
        ) {
            Image(painterResource(FrameIconsExtra.TrimRange), null, Modifier.size(13.dp), colorFilter = ColorFilter.tint(Color.White))
        }
    }
    if (item.durationMs > 0) {
        FrameText(
            formatTimestamp(item.durationMs),
            style = FrameTheme.typography.monoSmall.copy(textDirection = TextDirection.Ltr),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .background(Color.Black.copy(alpha = 0.55f), FrameShapes.pill)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
internal fun StartBar(count: Int, total: Int, enabled: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val click = if (enabled) Modifier.frameClickable(onClick = onClick) else Modifier
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .frameGlow(colors.primary, { if (enabled) 0.3f else 0f }, 14.dp)
                .clip(FrameShapes.pill)
                .background(FrameGradients.Brand)
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.06f))), FrameShapes.pill)
                .then(click),
            Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            Alignment.CenterVertically
        ) {
            Image(painterResource(FrameIconsExtra.Frames), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(Color.White))
            FrameText(TrimStrings.batchStart(), style = FrameTheme.typography.titleSmall, color = Color.White)
            FrameText(
                TrimStrings.batchVideos(count),
                style = FrameTheme.typography.label,
                color = Color.White,
                modifier = Modifier.background(Color.White.copy(alpha = 0.22f), FrameShapes.pill).padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}
