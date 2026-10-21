package com.frameout.app.viewer

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.core.edit.Transform
import com.frameout.core.edit.ViewGeometry

private const val DOUBLE_TAP_ZOOM = 3f
private const val ZOOMED = 1.05f
private const val SHARP_PIXEL_SIZE = 2f
private const val CROSSFADE_MS = 320

private class FadeTracker {
    var last: Bitmap? = null
    var key: Any? = null
}

@Composable
fun ViewerImage(
    bitmap: Bitmap?,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    cropMode: Boolean,
    fadeKey: Any? = null,
    colorFilter: ColorFilter? = null,
    onTransform: (Float, Float, Float) -> Unit,
    onSwipeDown: () -> Unit,
    onTap: () -> Unit,
    onLongPress: (Offset, Offset, Offset) -> Unit = { _, _, _ -> },
    description: String? = null,
    liveText: LiveTextState = LiveTextState(),
    onLiveSelect: (Int, Int) -> Unit = { _, _ -> },
    onLiveTap: (Int) -> Unit = {},
    transformNow: () -> Transform = { Transform(scale, offsetX, offsetY) }
) {
    if (bitmap == null) return
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val geo = remember(bitmap.width, bitmap.height, viewSize) {
        ViewGeometry(bitmap.width, bitmap.height, viewSize.width.toFloat(), viewSize.height.toFloat())
    }
    val currentTransform by rememberUpdatedState(transformNow)
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentScale by rememberUpdatedState(scale)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnSwipeDown by rememberUpdatedState(onSwipeDown)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentWords by rememberUpdatedState(liveText.words)
    val currentOnLiveSelect by rememberUpdatedState(onLiveSelect)
    val currentOnLiveTap by rememberUpdatedState(onLiveTap)
    val liveActive = liveText.active && liveText.words.isNotEmpty()
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val tracker = remember { FadeTracker() }
    val behind = remember(bitmap) {
        val prev = tracker.last
        val upgrade = prev != null && prev !== bitmap && tracker.key == fadeKey && !prev.isRecycled &&
            prev.width.toLong() * prev.height < bitmap.width.toLong() * bitmap.height
        tracker.last = bitmap
        tracker.key = fadeKey
        if (upgrade) prev else null
    }
    val behindImage = remember(behind) { behind?.asImageBitmap() }
    val fade = remember(bitmap) { Animatable(if (behind != null) 0f else 1f) }
    LaunchedEffect(bitmap) {
        if (behind != null) {
            val spec: AnimationSpec<Float> = if (reduceMotion) snap() else tween(CROSSFADE_MS)
            fade.animateTo(1f, spec)
        }
    }
    val sharp = geo.pixelSize(Transform(scale, offsetX, offsetY)) >= SHARP_PIXEL_SIZE
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
            .pointerInput(liveActive, cropMode, bitmap.width, bitmap.height, geo) {
                if (liveActive && !cropMode) detectLiveTextSelection(
                    words = { currentWords },
                    transform = { currentTransform() },
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                    holdToSelect = { currentTransform().scale > ZOOMED },
                    onSelect = { a, f -> currentOnLiveSelect(a, f) },
                    onTapWord = { currentOnLiveTap(it) },
                    onDoubleTap = { tap ->
                        val next = doubleTapTarget(geo, currentTransform(), tap, size)
                        currentOnTransform(next.scale, next.x, next.y)
                    }
                )
            }
            .pointerInput(cropMode, geo) {
                if (!cropMode) detectTapGestures(
                    onTap = { currentOnTap() },
                    onDoubleTap = { tap ->
                        val next = doubleTapTarget(geo, currentTransform(), tap, size)
                        currentOnTransform(next.scale, next.x, next.y)
                    },
                    onLongPress = { press -> currentOnLongPress(press, Offset(size.width / 2f, size.height / 2f), Offset(size.width.toFloat(), size.height.toFloat())) }
                )
            }
            .pointerInput(cropMode, geo) {
                if (!cropMode) detectVerticalSwipe({ currentScale <= ZOOMED }, { currentOnSwipeDown() })
            }
            .pointerInput(cropMode, geo) {
                if (cropMode) return@pointerInput
                detectViewportGestures({ currentScale > ZOOMED }) { centroid, pan, zoom ->
                    val next = geo.gesture(currentTransform(), zoom, centroid.x - size.width / 2f, centroid.y - size.height / 2f, pan.x, pan.y)
                    currentOnTransform(next.scale, next.x, next.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (behindImage != null && fade.value < 1f) {
            Image(
                bitmap = behindImage,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                filterQuality = FilterQuality.Low,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            )
        }
        Image(
            bitmap = imageBitmap,
            contentDescription = description,
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter,
            filterQuality = if (sharp) FilterQuality.None else FilterQuality.Low,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = fade.value
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )
    }
}

private fun doubleTapTarget(geo: ViewGeometry, current: Transform, tap: Offset, size: IntSize): Transform {
    if (current.scale > ZOOMED) return Transform.Identity
    return geo.zoomed(current, DOUBLE_TAP_ZOOM, tap.x - size.width / 2f, tap.y - size.height / 2f)
}

@Composable
fun ViewerNeighborImage(bitmap: Bitmap?, description: String? = null) {
    if (bitmap == null || bitmap.isRecycled) return
    Image(
        bitmap = remember(bitmap) { bitmap.asImageBitmap() },
        contentDescription = description,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
}

fun formatPts(ptsUs: Long): String {
    val totalMs = ptsUs / 1000
    val h = totalMs / 3_600_000
    val m = (totalMs % 3_600_000) / 60_000
    val s = (totalMs % 60_000) / 1000
    val ms = totalMs % 1000
    return if (h > 0) "%d:%02d:%02d.%03d".format(java.util.Locale.ROOT, h, m, s, ms)
    else "%02d:%02d.%03d".format(java.util.Locale.ROOT, m, s, ms)
}
