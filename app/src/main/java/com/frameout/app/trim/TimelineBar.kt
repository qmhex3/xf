package com.frameout.app.trim

import androidx.compose.foundation.border
import com.frameout.designsystem.theme.frameCardEdge
import com.frameout.designsystem.glass.frameGlow
import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.timeline.TrimRange
import java.util.Locale
import kotlin.math.roundToInt

private const val MIN_GAP_MS = 100L
private const val MAX_STRIP_PX = 60_000
private const val EDGE_TICK_MS = 16L
private const val EDGE_SPEED = 0.02f
private val HandleWidth = 18.dp
private val HitRadius = 28.dp
private val EdgeZone = 28.dp
private val BarHeight = 60.dp

private enum class Handle { None, Start, End }

private enum class Gesture { Pending, Drag, Pinch }

private class TimelinePaint(val primary: Color, val onPrimary: Color, val marker: Color, val queued: Color)

private class TrackGeometry(width: Float, val inset: Float, private val durationMs: Long, zoom: TimelineZoom = TimelineZoom()) {
    val trackWidth: Float = (width - inset * 2).coerceAtLeast(1f)
    val trackEnd: Float = inset + trackWidth
    private val viewStart: Float = zoom.startMs(durationMs)
    val viewSpan: Float = zoom.spanMs(durationMs).coerceAtLeast(1f)

    fun xOf(ms: Long): Float = inset + (ms.coerceIn(0L, durationMs) - viewStart) / viewSpan * trackWidth
    fun msOf(x: Float): Long = (viewStart + fractionOf(x) * viewSpan).toLong().coerceIn(0L, durationMs)
    fun fractionOf(x: Float): Float = ((x - inset) / trackWidth).coerceIn(0f, 1f)
}

@Composable
fun TimelineBar(
    durationMs: Long,
    positionMs: Long,
    range: TrimRange,
    selecting: Boolean,
    onSeek: (Long) -> Unit,
    onRangeChange: (TrimRange) -> Unit,
    thumbs: List<Bitmap?>,
    markers: List<Long> = emptyList(),
    modifier: Modifier = Modifier,
    queued: List<Long> = emptyList(),
    onScrub: (Boolean) -> Unit = {},
    slideWindow: Boolean = false
) {
    val colors = FrameTheme.colors
    val paint = TimelinePaint(colors.primary, colors.onPrimary, colors.success, colors.error)
    val dur = durationMs.coerceAtLeast(1)
    var active by remember { mutableStateOf(Handle.None) }
    var zoom by remember(dur) { mutableStateOf(TimelineZoom()) }
    var touching by remember { mutableStateOf(false) }
    val haptics = rememberFrameHaptics()
    LaunchedEffect(positionMs, dur) {
        if (!touching) zoom = zoom.revealing(positionMs, dur)
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.height(BarHeight).frameGlow(colors.primary, { if (colors.isDark) 0.16f else 0.08f }, 10.dp, { 0.2f }).clip(FrameShapes.sm).background(colors.containerHigh).border(1.dp, frameCardEdge(colors.primary, 0.35f), FrameShapes.sm).systemGestureExclusion()) {
            ThumbStrip(thumbs, { zoom }, Modifier.fillMaxSize().padding(horizontal = HandleWidth))
            Canvas(
                Modifier
                    .fillMaxSize()
                    .timelineSemantics(durationMs, positionMs, range, onSeek, onRangeChange)
                    .timelineGestures(
                        dur, range, selecting, slideWindow, { zoom }, { zoom = it },
                        onSeek, onRangeChange, onScrub, { active = it }, { haptics.perform(FrameHaptic.Tap) }, { touching = it }
                    )
            ) { drawTimeline(dur, positionMs, range, selecting, active, markers, queued, paint, zoom) }
            if (zoom.zoomed) {
                TimelineZoomBadge(zoom.scale, { zoom = TimelineZoom() }, Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = HandleWidth + 4.dp))
            }
        }
    }
}

@Composable
private fun ThumbStrip(thumbs: List<Bitmap?>, zoom: () -> TimelineZoom, modifier: Modifier) {
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
private fun TimelineZoomBadge(scale: Float, onReset: () -> Unit, modifier: Modifier) {
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

private fun Modifier.timelineGestures(
    durationMs: Long,
    range: TrimRange,
    selecting: Boolean,
    slideWindow: Boolean,
    zoomOf: () -> TimelineZoom,
    onZoom: (TimelineZoom) -> Unit,
    onSeek: (Long) -> Unit,
    onRangeChange: (TrimRange) -> Unit,
    onScrub: (Boolean) -> Unit,
    onActive: (Handle) -> Unit,
    onGrab: () -> Unit,
    onTouch: (Boolean) -> Unit
): Modifier = composed {
    val dur by rememberUpdatedState(durationMs)
    val current by rememberUpdatedState(range)
    val canSelect by rememberUpdatedState(selecting)
    val canSlide by rememberUpdatedState(slideWindow)
    val view by rememberUpdatedState(zoomOf)
    val setZoom by rememberUpdatedState(onZoom)
    val seek by rememberUpdatedState(onSeek)
    val change by rememberUpdatedState(onRangeChange)
    val scrub by rememberUpdatedState(onScrub)
    val activate by rememberUpdatedState(onActive)
    val grab by rememberUpdatedState(onGrab)
    val touch by rememberUpdatedState(onTouch)
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            touch(true)
            val handleWidth = HandleWidth.toPx()
            val edge = EdgeZone.toPx()
            val slop = viewConfiguration.touchSlop
            val width = size.width.toFloat()
            fun geometry() = TrackGeometry(width, handleWidth, dur, view())
            val start = geometry()
            val handle = if (canSelect) pickHandle(down.position.x, start, current, HitRadius.toPx(), handleWidth) else Handle.None
            val anchor = when (handle) {
                Handle.Start -> down.position.x - start.xOf(current.startMs)
                Handle.End -> down.position.x - start.xOf(current.endMs)
                Handle.None -> 0f
            }
            val slide = canSelect && canSlide && handle == Handle.None &&
                down.position.x in start.xOf(current.startMs)..start.xOf(current.endMs)
            val slideStart = current.startMs
            val slideSpan = current.endMs - current.startMs
            if (handle != Handle.None) {
                activate(handle)
                grab()
            }
            fun slideTo(x: Float) {
                val g = geometry()
                val delta = ((x - down.position.x) / g.trackWidth * g.viewSpan).toLong()
                val first = (slideStart + delta).coerceIn(0L, (dur - slideSpan).coerceAtLeast(0L))
                change(current.copy(startMs = first, endMs = first + slideSpan, useFull = false))
                seek(first)
            }
            fun moveTo(x: Float) {
                applyDrag(handle, geometry().msOf(x - anchor), current, seek, change)
            }
            val follow: (Float) -> Unit = if (slide) ::slideTo else ::moveTo
            fun edgeScroll(x: Float) {
                val g = geometry()
                val intoStart = g.inset + edge - x
                val intoEnd = x - (g.trackEnd - edge)
                val push = when {
                    intoStart > 0f -> -(intoStart / edge)
                    intoEnd > 0f -> intoEnd / edge
                    else -> 0f
                }.coerceIn(-1.5f, 1.5f)
                if (push == 0f) return
                val before = view()
                val after = before.scrolledBy(push * EDGE_SPEED)
                if (after != before) {
                    setZoom(after)
                    follow(x)
                }
            }

            var mode = Gesture.Pending
            var lastX = down.position.x
            loop@ while (true) {
                val watching = mode == Gesture.Drag && !slide && view().zoomed
                val event = if (watching) withTimeoutOrNull(EDGE_TICK_MS) { awaitPointerEvent() } else awaitPointerEvent()
                if (event == null) {
                    edgeScroll(lastX)
                    continue@loop
                }
                val fingers = event.changes.count { it.pressed }
                if (fingers == 0) break@loop
                if (mode != Gesture.Pinch && fingers >= 2) {
                    if (mode == Gesture.Drag) scrub(false)
                    activate(Handle.None)
                    mode = Gesture.Pinch
                }
                when (mode) {
                    Gesture.Pinch -> if (fingers >= 2) {
                        val g = geometry()
                        val zoomChange = event.calculateZoom()
                        val pan = event.calculatePan()
                        if (zoomChange != 1f || pan.x != 0f) {
                            val at = g.fractionOf(event.calculateCentroid(useCurrent = false).x)
                            setZoom(view().transform(at, pan.x / g.trackWidth, zoomChange, dur))
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                    Gesture.Pending -> {
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break@loop
                        lastX = pointer.position.x
                        if ((pointer.position - down.position).getDistance() > slop) {
                            mode = Gesture.Drag
                            scrub(true)
                            if (slide) grab()
                            follow(lastX)
                            pointer.consume()
                        }
                    }
                    Gesture.Drag -> {
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break@loop
                        lastX = pointer.position.x
                        follow(lastX)
                        pointer.consume()
                    }
                }
            }
            when (mode) {
                Gesture.Pending -> when (handle) {
                    Handle.Start -> seek(current.startMs)
                    Handle.End -> seek(current.endMs)
                    Handle.None -> seek(geometry().msOf(down.position.x))
                }
                Gesture.Drag -> scrub(false)
                Gesture.Pinch -> Unit
            }
            activate(Handle.None)
            touch(false)
        }
    }
}

private fun pickHandle(x: Float, geometry: TrackGeometry, range: TrimRange, hit: Float, handleWidth: Float): Handle {
    val startX = geometry.xOf(range.startMs)
    val endX = geometry.xOf(range.endMs)
    val startShown = startX >= geometry.inset - 0.5f && startX <= geometry.trackEnd + handleWidth
    val endShown = endX >= geometry.inset - handleWidth && endX <= geometry.trackEnd + 0.5f
    val dStart = if (startShown) kotlin.math.abs(x - (startX - handleWidth / 2)) else Float.MAX_VALUE
    val dEnd = if (endShown) kotlin.math.abs(x - (endX + handleWidth / 2)) else Float.MAX_VALUE
    return when {
        dStart <= hit && dStart <= dEnd -> Handle.Start
        dEnd <= hit -> Handle.End
        else -> Handle.None
    }
}

private fun applyDrag(target: Handle, ms: Long, range: TrimRange, onSeek: (Long) -> Unit, onRangeChange: (TrimRange) -> Unit) {
    when (target) {
        Handle.Start -> {
            val start = ms.coerceAtMost(range.endMs - MIN_GAP_MS).coerceAtLeast(0L)
            onRangeChange(range.copy(startMs = start, useFull = false))
            onSeek(start)
        }
        Handle.End -> {
            val end = ms.coerceAtLeast(range.startMs + MIN_GAP_MS)
            onRangeChange(range.copy(endMs = end, useFull = false))
            onSeek(end)
        }
        Handle.None -> onSeek(ms)
    }
}

private fun DrawScope.drawTimeline(
    dur: Long,
    positionMs: Long,
    range: TrimRange,
    selecting: Boolean,
    active: Handle,
    markers: List<Long>,
    queued: List<Long>,
    paint: TimelinePaint,
    zoom: TimelineZoom
) {
    val geometry = TrackGeometry(size.width, HandleWidth.toPx(), dur, zoom)
    val h = size.height
    val hw = HandleWidth.toPx()
    markers.forEach { drawDot(geometry.xOf(it), h - 9.dp.toPx(), paint.marker) }
    queued.forEach { drawDot(geometry.xOf(it), 9.dp.toPx(), paint.queued) }
    if (selecting) {
        val startX = geometry.xOf(range.startMs)
        val endX = geometry.xOf(range.endMs).coerceAtLeast(startX)
        val from = startX.coerceIn(geometry.inset, geometry.trackEnd)
        val to = endX.coerceIn(geometry.inset, geometry.trackEnd)
        val dim = Color.Black.copy(alpha = 0.58f)
        drawRect(dim, Offset(geometry.inset, 0f), Size((from - geometry.inset).coerceAtLeast(0f), h))
        drawRect(dim, Offset(to, 0f), Size((geometry.trackEnd - to).coerceAtLeast(0f), h))
        val edge = 3.dp.toPx()
        drawRect(paint.primary, Offset(from, 0f), Size(to - from, edge))
        drawRect(paint.primary, Offset(from, h - edge), Size(to - from, edge))
        if (startX >= geometry.inset - 0.5f && startX <= geometry.trackEnd + hw) drawHandle(startX - hw, h, active == Handle.Start, paint)
        if (endX >= geometry.inset - hw && endX <= geometry.trackEnd + 0.5f) drawHandle(endX, h, active == Handle.End, paint)
    }
    drawPlayhead(geometry.xOf(positionMs), h)
}

private fun DrawScope.drawDot(x: Float, y: Float, color: Color) {
    drawCircle(Color.Black.copy(alpha = 0.55f), 4.5.dp.toPx(), Offset(x, y))
    drawCircle(color, 3.dp.toPx(), Offset(x, y))
}

private fun DrawScope.drawHandle(x: Float, h: Float, active: Boolean, paint: TimelinePaint) {
    val hw = HandleWidth.toPx()
    val radius = CornerRadius(5.dp.toPx())
    drawRoundRect(paint.primary, Offset(x, 0f), Size(hw, h), radius)
    if (active) drawRoundRect(Color.White, Offset(x, 0f), Size(hw, h), radius, style = Stroke(2.dp.toPx()))
    val grip = Size(2.dp.toPx(), h * 0.36f)
    drawRoundRect(paint.onPrimary, Offset(x + hw / 2 - grip.width / 2, h * 0.32f), grip, CornerRadius(1.dp.toPx()))
}

private fun DrawScope.drawPlayhead(x: Float, h: Float) {
    drawLine(Color.Black.copy(alpha = 0.4f), Offset(x, 0f), Offset(x, h), strokeWidth = 5.dp.toPx())
    drawLine(Color.White, Offset(x, 0f), Offset(x, h), strokeWidth = 2.dp.toPx())
    drawCircle(Color.White, 5.dp.toPx(), Offset(x, 5.dp.toPx()))
}
