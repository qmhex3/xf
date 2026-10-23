package com.frameout.app.picker

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import com.frameout.app.thumb.ThumbPlaceholder
import com.frameout.app.thumb.rememberThumb
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.frameout.designsystem.a11y.LocalSystemA11y
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.frameout.core.media.MediaItem
import com.frameout.core.media.MediaKind
import com.frameout.core.media.MediaThumbLoader
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.PickerStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import java.util.Locale
import kotlinx.coroutines.CancellationException

internal object PickerGrid {
    val MinCell: Dp = 100.dp
    val Gap: Dp = 6.dp
    val Overlay: Dp = 8.dp
    val Scrim: Color = Color.Black.copy(alpha = 0.6f)
    fun padding(bottom: Dp, top: Dp = 0.dp) = PaddingValues(start = 12.dp, top = top + 8.dp, end = 12.dp, bottom = bottom)
}

@Composable
fun rememberMediaThumb(item: MediaItem, maxSize: Int = 256): State<Bitmap?> {
    val context = LocalContext.current
    return rememberThumb(item.uri, { MediaThumbLoader.peek(item, maxSize) }, { MediaThumbLoader.load(context, item, maxSize) })
}

private class GridThumbState(val bitmap: State<Bitmap?>, val finished: State<Boolean>)

@Composable
private fun rememberGridThumb(item: MediaItem): GridThumbState {
    val context = LocalContext.current
    val finished = remember(item.uri) { mutableStateOf(false) }
    val bitmap = rememberThumb(item.uri, { MediaThumbLoader.peek(item, 256) }, {
        try {
            MediaThumbLoader.load(context, item, 256)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } finally {
            finished.value = true
        }
    })
    return GridThumbState(bitmap, finished)
}

@Composable
private fun rememberSelectAction(item: MediaItem, selected: Boolean, coords: Array<LayoutCoordinates?>, onClick: () -> Unit): () -> Unit {
    val flight = LocalPickerFlight.current
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    return {
        if (!selected && !reduceMotion && flight != null) {
            val c = coords[0]
            if (c != null && c.isAttached) flight.launch(item, Rect(c.positionInWindow(), c.size.toSize()), MediaThumbLoader.peek(item, 256))
        }
        onClick()
    }
}

@Composable
fun MediaGridItem(item: MediaItem, selectionIndex: Int?, selectionMode: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val selected = selectionIndex != null
    val gridThumb = rememberGridThumb(item)
    val thumb by gridThumb.bitmap
    val fade = animateFloatAsState(if (thumb != null) 1f else 0f, tween(180), label = "pickerThumbFade")
    val inset by animateDpAsState(if (selected) 6.dp else 0.dp, frameSpring(), label = "pickerInset")
    val ring by animateColorAsState(if (selected) colors.primary else Color.Transparent, frameSpring(), label = "pickerRing")
    val description = remember(item.uri) { cellDescription(item) }
    val selectedState = selectionIndex?.let { PickerStrings.selectedState(it + 1) }
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val select = rememberSelectAction(item, selected, coords, onClick)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onGloballyPositioned { coords[0] = it }
            .frameSelectable(selected, Role.Checkbox, select)
            .semantics {
                contentDescription = description
                if (selectedState != null) stateDescription = selectedState
            }
            .clip(FrameShapes.sm)
            .background(colors.container)
            .border(2.dp, ring, FrameShapes.sm)
    ) {
        GridThumb(item, thumb, fade, gridThumb.finished.value, Modifier.fillMaxSize().padding(inset.coerceAtLeast(0.dp)))
        MetaPill(item, Modifier.align(Alignment.BottomStart).padding(PickerGrid.Overlay))
        SelectionMark(selectionIndex, selectionMode, Modifier.align(Alignment.TopEnd).padding(PickerGrid.Overlay))
    }
}

@Composable
private fun GridThumb(item: MediaItem, bitmap: Bitmap?, fade: State<Float>, finished: Boolean, modifier: Modifier) {
    Box(modifier.clip(FrameShapes.xs).background(FrameTheme.colors.containerHigh)) {
        if (bitmap != null) {
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(image, contentDescription = null, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }, contentScale = ContentScale.Crop)
        } else {
            ThumbPlaceholder(icon = item.kind.icon(), pulsing = !finished)
        }
    }
}

@Composable
private fun MetaPill(item: MediaItem, modifier: Modifier) {
    val kind = item.kind
    if (kind == MediaKind.Image) return
    val text = when (kind) {
        MediaKind.Gif -> "GIF"
        MediaKind.LivePhoto -> FrameStrings.mediaLive()
        MediaKind.MotionPhoto -> FrameStrings.mediaMotion()
        MediaKind.Animated -> FrameStrings.mediaAnimated()
        MediaKind.Video -> if (item.durationMs > 0) formatDuration(item.durationMs) else null
        MediaKind.Image -> null
    }
    Row(
        modifier.background(PickerGrid.Scrim, FrameShapes.pill).padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(kind.icon()), null, Modifier.size(13.dp), colorFilter = ColorFilter.tint(Color.White))
        if (text != null) {
            Spacer(Modifier.width(4.dp))
            FrameText(text, style = FrameTheme.typography.monoSmall, color = Color.White, maxLines = 1)
        }
    }
}

@Composable
private fun SelectionMark(index: Int?, selectionMode: Boolean, modifier: Modifier) {
    val colors = FrameTheme.colors
    if (index != null) {
        Box(
            modifier.defaultMinSize(24.dp, 24.dp).background(colors.primary, FrameShapes.pill).padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            FrameText("${index + 1}", style = FrameTheme.typography.label, color = colors.onPrimary)
        }
    } else if (selectionMode) {
        Box(
            modifier
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
        )
    }
}

private fun cellDescription(item: MediaItem): String {
    val type = when (item.kind) {
        MediaKind.Gif -> "GIF"
        MediaKind.LivePhoto -> FrameStrings.mediaLive()
        MediaKind.MotionPhoto -> FrameStrings.mediaMotion()
        MediaKind.Animated -> FrameStrings.mediaAnimated()
        MediaKind.Video -> FrameStrings.mediaVideo()
        MediaKind.Image -> null
    }
    val parts = listOfNotNull(item.displayName, type, if (item.durationMs > 0) formatDuration(item.durationMs) else null)
    return parts.joinToString(", ")
}

private fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
