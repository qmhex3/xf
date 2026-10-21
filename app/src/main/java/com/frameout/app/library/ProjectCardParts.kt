package com.frameout.app.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.thumb.ThumbPlaceholder
import com.frameout.core.db.ProjectEntity
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameColorScheme
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import java.util.Locale

internal fun isLive(status: String) = status == "Extracting" || status == "Indexing"

@Composable
internal fun ProjectThumb(thumb: android.graphics.Bitmap?, project: ProjectEntity) {
    val colors = FrameTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.size(84.dp).clip(shape).background(colors.containerHigh).border(1.dp, frameHairline(), shape), Alignment.Center) {
        thumb?.let { Image(it.asImageBitmap(), null, Modifier.size(84.dp), contentScale = ContentScale.Crop) } ?: ThumbPlaceholder(iconSize = 28.dp)
        if (isLive(project.status)) PulseDot(Modifier.align(Alignment.TopStart).padding(6.dp))
        if (project.durationMs > 0) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(5.dp).background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(7.dp)).padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                FrameText(formatDuration(project.durationMs), style = FrameTheme.typography.monoSmall.copy(fontSize = 10.sp), color = Color.White)
            }
        }
    }
}

@Composable
private fun PulseDot(modifier: Modifier) {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val a = if (reduce) 1f else rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dotAlpha"
    ).value
    Box(modifier.background(Color.Black.copy(alpha = 0.55f), CircleShape).padding(5.dp)) {
        Box(Modifier.size(8.dp).alpha(a).background(colors.success, CircleShape))
    }
}

@Composable
internal fun StatusCorner(status: String) {
    val color = statusColor(status, FrameTheme.colors)
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.28f), shape)
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (status == "Completed") Image(painterResource(FrameIcons.Check), null, Modifier.size(13.dp), colorFilter = ColorFilter.tint(color))
        else Box(Modifier.size(7.dp).background(color, CircleShape))
        FrameText(statusText(status), style = FrameTheme.typography.label, color = color, maxLines = 1)
    }
}

@Composable
internal fun FrameCountChip(count: String, percent: Int?, description: String) {
    val colors = FrameTheme.colors
    val shape = RoundedCornerShape(10.dp)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier
                .clip(shape)
                .background(colors.onSurface.copy(alpha = 0.06f))
                .border(1.dp, frameHairline(), shape)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .semantics(mergeDescendants = true) { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(painterResource(FrameIconsExtra.Keyframe), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(colors.onSurfaceVariant))
            FrameText(count, style = FrameTheme.typography.monoSmall, color = colors.onSurface, maxLines = 1)
            FrameText(FrameStrings.frames().lowercase(), style = FrameTheme.typography.monoSmall, color = colors.onSurfaceVariant, maxLines = 1)
            percent?.let { FrameText("$it%", style = FrameTheme.typography.monoSmall, color = colors.primary, maxLines = 1) }
        }
    }
}

@Composable
internal fun DeleteAction(onDelete: () -> Unit, modifier: Modifier = Modifier) {
    CardAction(FrameIcons.Trash, FrameStrings.delete(), FrameTheme.colors.onSurfaceVariant, false, onDelete, modifier)
}

internal fun hasStateAction(project: ProjectEntity): Boolean =
    isLive(project.status) || project.status == "Paused" || project.status == "Failed"

@Composable
internal fun StateAction(project: ProjectEntity, onResume: () -> Unit, onPause: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    when {
        isLive(project.status) -> CardAction(FrameIcons.Pause, FrameStrings.pause(), colors.primary, true, onPause, modifier)
        project.status == "Paused" || project.status == "Failed" -> CardAction(FrameIconsExtra.Resume, FrameStrings.resume(), colors.primary, true, onResume, modifier)
    }
}

@Composable
private fun CardAction(icon: Int, label: String, tint: Color, tonal: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(48.dp)
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = label },
        Alignment.Center
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (tonal) tint.copy(alpha = 0.16f) else FrameTheme.colors.onSurface.copy(alpha = 0.05f)),
            Alignment.Center
        ) {
            Image(painterResource(icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(tint))
        }
    }
}

internal fun statusColor(status: String, colors: FrameColorScheme): Color = when (status) {
    "Completed" -> colors.success
    "Paused" -> colors.warning
    "Extracting", "Indexing" -> colors.primary
    "Failed", "Cancelled" -> colors.error
    else -> colors.onSurfaceVariant
}

internal fun statusText(value: String) = when (value) {
    "Queued" -> FrameStrings.queued(); "Indexing" -> FrameStrings.indexing(); "Extracting" -> FrameStrings.extracting()
    "Paused" -> FrameStrings.paused(); "Completed" -> FrameStrings.completed(); "Cancelled" -> FrameStrings.cancelled()
    "Failed" -> FrameStrings.failed(); else -> FrameStrings.working()
}

internal fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}
