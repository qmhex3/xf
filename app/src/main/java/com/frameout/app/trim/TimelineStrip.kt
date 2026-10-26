package com.frameout.app.trim

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun ThumbStrip(thumbs: List<Bitmap?>, zoom: () -> TimelineZoom, modifier: Modifier) {
    val slotColor = FrameTheme.colors.container
    Box(modifier.clipToBounds()) {
        Row(Modifier.zoomedStrip(zoom)) {
            repeat(thumbs.size.coerceAtLeast(THUMB_SLOTS)) { slot ->
                Crossfade(thumbs.getOrNull(slot), Modifier.weight(1f).fillMaxHeight(), animationSpec = tween(220), label = "thumb") { bitmap ->
                    if (bitmap != null) {
                        val image = remember(bitmap) { bitmap.asImageBitmap() }
                        Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize().background(slotColor))
                    }
                }
            }
        }
    }
}

private fun Modifier.zoomedStrip(zoom: () -> TimelineZoom): Modifier = layout { measurable, constraints ->
    val z = zoom()
    val full = (constraints.maxWidth * z.scale).roundToInt().coerceAtMost(MAX_STRIP_PX).coerceAtLeast(constraints.maxWidth)
    val placeable = measurable.measure(Constraints.fixed(full, constraints.maxHeight))
    val shift = (z.start * full).roundToInt().coerceIn(0, (full - constraints.maxWidth).coerceAtLeast(0))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(-shift, 0) }
}

@Composable
internal fun TimelineZoomBadge(scale: Float, onReset: () -> Unit, modifier: Modifier) {
    val label = String.format(Locale.US, "%.1f\u00d7", scale)
    val description = FrameStrings.zoomReset(label)
    val shape = FrameShapes.pill
    Row(
        modifier
            .height(24.dp)
            .clip(shape)
            .background(Color(0xB8101216), shape)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .frameClickable(onClickLabel = description, onClick = onReset)
            .semantics { contentDescription = description }
            .padding(horizontal = 8.dp),
        Arrangement.spacedBy(4.dp),
        Alignment.CenterVertically
    ) {
        Image(painterResource(FrameIconsExtra.Fit), null, Modifier.size(12.dp), colorFilter = ColorFilter.tint(Color.White))
        FrameText(label, style = FrameTheme.typography.label, color = Color.White)
    }
}
