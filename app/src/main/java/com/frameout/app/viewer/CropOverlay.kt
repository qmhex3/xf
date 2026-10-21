package com.frameout.app.viewer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameA11y
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameTheme

private class CropDragCallbacks(
    val box: () -> CropBox,
    val onStart: (CropHandle) -> Unit,
    val onChange: (CropBox) -> Unit,
    val onEnd: () -> Unit
)

@Composable
fun CropOverlay(
    cropL: Float,
    cropT: Float,
    cropR: Float,
    cropB: Float,
    aspect: Float,
    onChange: (Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var active by remember { mutableStateOf(CropHandle.None) }
    var dragging by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val reduce = LocalSystemA11y.current.reduceMotion
    val spec: AnimationSpec<Float> = if (dragging || reduce) snap() else tween(260, easing = FastOutSlowInEasing)
    val l by animateFloatAsState(cropL, spec, label = "cropL")
    val t by animateFloatAsState(cropT, spec, label = "cropT")
    val r by animateFloatAsState(cropR, spec, label = "cropR")
    val b by animateFloatAsState(cropB, spec, label = "cropB")
    val gridAlpha by animateFloatAsState(if (dragging) 1f else 0f, tween(180), label = "cropGrid")
    val handleScale by animateFloatAsState(if (dragging) 1.18f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "cropHandle")
    val appear by animateFloatAsState(if (entered) 1f else 0f, tween(if (reduce) 0 else 280), label = "cropAppear")
    val latest = rememberUpdatedState(CropBox(cropL, cropT, cropR, cropB))
    val latestChange = rememberUpdatedState(onChange)
    val accent = FrameTheme.colors.onDarkAccent
    val callbacks = remember {
        CropDragCallbacks(
            box = { latest.value },
            onStart = { active = it; dragging = true },
            onChange = { latestChange.value(it.l, it.t, it.r, it.b) },
            onEnd = { dragging = false; active = CropHandle.None }
        )
    }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = appear }
            .semantics { contentDescription = FrameA11y.cropArea() }
            .pointerInput(aspect) { detectCropDrag(aspect, callbacks) }
    ) {
        val img = fitRect(size, aspect)
        val c = CropBox(l, t, r, b).toRect(img)
        drawCropDim(img, c)
        drawCropFrame(c, gridAlpha)
        drawCropHandles(c, accent, active, handleScale)
    }
}

private suspend fun PointerInputScope.detectCropDrag(aspect: Float, cb: CropDragCallbacks) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val img: Rect = fitRect(androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat()), aspect)
        val start = cb.box()
        val handle = hitCrop(down.position, img, start, 30.dp.toPx(), 24.dp.toPx())
        if (handle == CropHandle.None) return@awaitEachGesture
        down.consume()
        cb.onStart(handle)
        var total = androidx.compose.ui.geometry.Offset.Zero
        var pressed = true
        while (pressed) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            val delta = change.positionChange()
            if (delta != androidx.compose.ui.geometry.Offset.Zero) {
                total += delta
                change.consume()
                cb.onChange(dragCrop(handle, start, total.x / img.width, total.y / img.height))
            }
            pressed = change.pressed
        }
        cb.onEnd()
    }
}
