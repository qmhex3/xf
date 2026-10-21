package com.frameout.app.compare

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.app.viewer.PixelGrid
import com.frameout.app.viewer.detectViewportGestures
import com.frameout.core.edit.ViewGeometry
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private const val SHARP_PIXEL_SIZE = 3f
private const val SPLIT_MIN = 0.02f
private const val SPLIT_MAX = 0.98f

private fun splitHaptic(prev: Float, next: Float): FrameHaptic? = when {
    prev == next -> null
    (prev - 0.5f) * (next - 0.5f) <= 0f -> FrameHaptic.Select
    next == SPLIT_MIN && prev != SPLIT_MIN -> FrameHaptic.Select
    next == SPLIT_MAX && prev != SPLIT_MAX -> FrameHaptic.Select
    else -> null
}

@Composable
private fun ZoomImage(item: PaneItem, zoom: CompareZoom) {
    val bitmap = item.bitmap
    if (bitmap == null || bitmap.isRecycled) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            FrameText(CompareStrings.loadFailed(), style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant)
        }
        return
    }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val geo = remember(bitmap.width, bitmap.height, size) { ViewGeometry(bitmap.width, bitmap.height, size.width.toFloat(), size.height.toFloat()) }
    val local = zoom.local(size)
    Box(Modifier.fillMaxSize().onSizeChanged { size = it }) {
        Image(
            bitmap = image,
            contentDescription = item.description,
            contentScale = ContentScale.Fit,
            filterQuality = if (geo.pixelSize(local) >= SHARP_PIXEL_SIZE) FilterQuality.None else FilterQuality.Low,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val lt = zoom.local(size)
                scaleX = lt.scale
                scaleY = lt.scale
                translationX = lt.x
                translationY = lt.y
            }
        )
        PixelGrid(geo, local)
    }
}

@Composable
internal fun ZoomPane(item: PaneItem, zoom: CompareZoom, modifier: Modifier, primary: Boolean = false) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(item.bitmap, size, primary, zoom) {
        val b = item.bitmap?.takeIf { !it.isRecycled }
        if (primary && b != null) zoom.configure(b.width, b.height, size)
    }
    Box(
        modifier
            .clip(FrameShapes.sm)
            .background(FrameTheme.colors.container)
            .onSizeChanged { size = it }
            .pointerInput(zoom) { detectTapGestures(onDoubleTap = { zoom.doubleTap(it, this.size) }) }
            .pointerInput(zoom) { detectViewportGestures({ true }) { c, p, z -> zoom.gesture(c, p, z, this.size) } }
    ) {
        ZoomImage(item, zoom)
        PaneTag(item, Modifier.align(Alignment.BottomStart).padding(8.dp))
    }
}

@Composable
internal fun SliderCompare(a: PaneItem, b: PaneItem, zoom: CompareZoom, modifier: Modifier) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        var size by remember { mutableStateOf(IntSize.Zero) }
        val split = remember { mutableFloatStateOf(0.5f) }
        var dragging by remember { mutableStateOf(false) }
        CompareIntroNudge(split, a.bitmap != null && b.bitmap != null) { dragging }
        LaunchedEffect(a.bitmap, size) {
            val bm = a.bitmap?.takeIf { !it.isRecycled }
            if (bm != null) zoom.configure(bm.width, bm.height, size)
        }
        Box(
            modifier
                .clip(FrameShapes.sm)
                .background(FrameTheme.colors.container)
                .onSizeChanged { size = it }
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { zoom.doubleTap(it, this.size) }) }
                .pointerInput(Unit) { detectViewportGestures({ true }) { c, p, z -> if (!dragging) zoom.gesture(c, p, z, this.size) } }
        ) {
            Box(Modifier.fillMaxSize().drawWithContent {
                clipRect(left = 0f, top = 0f, right = split.floatValue * this.size.width, bottom = this.size.height) { this@drawWithContent.drawContent() }
            }) { ZoomImage(a, zoom) }
            Box(Modifier.fillMaxSize().drawWithContent {
                clipRect(left = split.floatValue * this.size.width, top = 0f, right = this.size.width, bottom = this.size.height) { this@drawWithContent.drawContent() }
            }) { ZoomImage(b, zoom) }
            PaneTag(a, Modifier.align(Alignment.BottomStart).padding(8.dp))
            PaneTag(b, Modifier.align(Alignment.BottomEnd).padding(8.dp))
            SliderHandle(split, { size.width }) { dragging = it }
        }
    }
}

@Composable
private fun SliderHandle(split: MutableFloatState, widthPx: () -> Int, onDragging: (Boolean) -> Unit) {
    val handleHalf = with(LocalDensity.current) { 24.dp.roundToPx() }
    val haptics = rememberFrameHaptics()
    Box(
        Modifier
            .fillMaxHeight()
            .width(48.dp)
            .offset { IntOffset((split.floatValue * widthPx()).toInt() - handleHalf, 0) }
            .semantics { contentDescription = CompareStrings.divider() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { onDragging(true); haptics.perform(FrameHaptic.DragStart) },
                    onDragEnd = { onDragging(false); haptics.perform(FrameHaptic.DragEnd) },
                    onDragCancel = { onDragging(false) }
                ) { change, dx ->
                    change.consume()
                    val w = widthPx()
                    if (w > 0) {
                        val next = (split.floatValue + dx / w).coerceIn(SPLIT_MIN, SPLIT_MAX)
                        splitHaptic(split.floatValue, next)?.let(haptics::perform)
                        split.floatValue = next
                    }
                }
            },
        Alignment.Center
    ) {
        Box(Modifier.fillMaxHeight().width(2.dp).background(Color.White.copy(alpha = 0.9f)))
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).border(1.5.dp, FrameTheme.colors.primary, CircleShape), Alignment.Center) {
            DividerGlyph()
        }
    }
}

@Composable
private fun DividerGlyph() {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val left = Path().apply { moveTo(w * 0.40f, h * 0.2f); lineTo(w * 0.08f, h * 0.5f); lineTo(w * 0.40f, h * 0.8f) }
        val right = Path().apply { moveTo(w * 0.60f, h * 0.2f); lineTo(w * 0.92f, h * 0.5f); lineTo(w * 0.60f, h * 0.8f) }
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(left, Color.White, style = stroke)
        drawPath(right, Color.White, style = stroke)
    }
}
