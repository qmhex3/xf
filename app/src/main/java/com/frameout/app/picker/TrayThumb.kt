package com.frameout.app.picker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.frameout.app.thumb.ThumbPlaceholder
import com.frameout.core.media.MediaItem
import com.frameout.core.util.PickerStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.FrameHaptics
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

internal class TrayAnim(
    val pop: Animatable<Float, AnimationVector1D>,
    val bump: Animatable<Float, AnimationVector1D>,
    val arrival: Animatable<Float, AnimationVector1D>
)

@Composable
private fun rememberTrayAnim(flying: Boolean, highlighted: Boolean, haptics: FrameHaptics): TrayAnim {
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val anim = remember { TrayAnim(Animatable(if (reduceMotion || flying) 1f else 0.4f), Animatable(1f), Animatable(0f)) }
    var wasFlying by remember { mutableStateOf(flying) }
    LaunchedEffect(Unit) {
        if (anim.pop.value < 1f) anim.pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
    }
    LaunchedEffect(flying) {
        if (wasFlying && !flying) {
            anim.bump.snapTo(1.1f)
            anim.bump.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
            haptics.perform(FrameHaptic.Confirm)
        }
        wasFlying = flying
    }
    LaunchedEffect(highlighted) {
        if (highlighted && reduceMotion) {
            anim.arrival.snapTo(1f)
            anim.arrival.animateTo(0f, tween(500))
        }
    }
    return anim
}

private fun Modifier.trayThumbLayer(uri: String, flight: PickerFlight?, anim: TrayAnim, drag: TrayDrag, axis: Float): Modifier =
    graphicsLayer {
        val dragging = drag.uri == uri
        val scale = anim.pop.value * anim.bump.value * (if (dragging) 1.08f else 1f)
        scaleX = scale
        scaleY = scale
        translationX = if (dragging) drag.offset * axis else 0f
        alpha = if (flight?.isFlying(uri) == true) 0f else ((anim.pop.value - 0.4f) / 0.6f).coerceIn(0f, 1f)
    }

@Composable
private fun Modifier.trayDragGesture(uri: String, drag: TrayDrag, axis: Float, onDrag: (String, Float) -> Unit): Modifier {
    val haptics = rememberFrameHaptics()
    val currentDrag by rememberUpdatedState(onDrag)
    return pointerInput(uri, axis) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                drag.begin(uri)
                haptics.perform(FrameHaptic.Confirm)
            },
            onDrag = { change, amount ->
                change.consume()
                currentDrag(uri, amount.x * axis)
            },
            onDragEnd = { drag.end() },
            onDragCancel = { drag.end() }
        )
    }
}

@Composable
internal fun LazyItemScope.TrayThumb(
    item: MediaItem,
    highlighted: Boolean,
    drag: TrayDrag,
    onDrag: (String, Float) -> Unit,
    onRemove: () -> Unit
) {
    val thumb by rememberMediaThumb(item, 256)
    val flight = LocalPickerFlight.current
    val haptics = rememberFrameHaptics()
    val axis = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val uri = item.uri
    val anim = rememberTrayAnim(flight?.isFlying(uri) == true, highlighted, haptics)
    val dragging = drag.uri == uri
    DisposableEffect(uri) { onDispose { flight?.trayBounds?.remove(uri) } }
    val ring = FrameTheme.colors.primary.copy(alpha = if (dragging) 0.9f else anim.arrival.value)
    Box(
        Modifier
            .then(if (dragging) Modifier.zIndex(1f) else Modifier.animateItem())
            .onGloballyPositioned { flight?.trayBounds?.put(uri, Rect(it.positionInWindow(), it.size.toSize())) }
            .trayThumbLayer(uri, flight, anim, drag, axis)
            .size(48.dp)
            .trayDragGesture(uri, drag, axis, onDrag)
            .clip(FrameShapes.xs)
            .background(FrameTheme.colors.containerHigh)
            .border(2.dp, ring, FrameShapes.xs)
    ) {
        val bitmap = thumb
        if (bitmap != null) {
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            ThumbPlaceholder(iconSize = 20.dp, icon = item.kind.icon())
        }
        TrayRemoveBadge(PickerStrings.removeItem(item.displayName), onRemove, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun TrayRemoveBadge(label: String, onRemove: () -> Unit, modifier: Modifier) {
    val haptics = rememberFrameHaptics()
    Box(
        modifier
            .size(30.dp)
            .clickable(onClickLabel = label, role = Role.Button) {
                haptics.perform(FrameHaptic.Tap)
                onRemove()
            }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.TopEnd
    ) {
        Box(Modifier.padding(3.dp).size(16.dp).background(PickerGrid.Scrim, CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(FrameIcons.Close), null, Modifier.size(10.dp), colorFilter = ColorFilter.tint(Color.White))
        }
    }
}
