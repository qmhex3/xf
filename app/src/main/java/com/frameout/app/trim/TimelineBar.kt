package com.frameout.app.trim

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardEdge
import com.frameout.engine.timeline.TrimRange

internal const val MIN_GAP_MS = 100L
internal const val MAX_STRIP_PX = 60_000
private const val EDGE_TICK_MS = 16L
private const val EDGE_SPEED = 0.02f
internal val HandleWidth = 18.dp
private val HitRadius = 28.dp
private val EdgeZone = 28.dp
private val BarHeight = 60.dp

internal enum class Handle { None, Start, End }

private enum class Gesture { Pending, Drag, Pinch }

internal class TimelinePaint(val primary: Color, val onPrimary: Color, val marker: Color, val queued: Color)

internal class TrackGeometry(width: Float, val inset: Float, private val durationMs: Long, zoom: TimelineZoom = TimelineZoom()) {
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
