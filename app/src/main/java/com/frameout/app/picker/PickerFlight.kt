package com.frameout.app.picker

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.frameout.app.thumb.ThumbPlaceholder
import com.frameout.core.media.MediaItem
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

@Stable
internal class PickerFlight {
    class Flight(val item: MediaItem, val from: Rect, val durationMs: Int, val bitmap: Bitmap?)

    val flights = mutableStateListOf<Flight>()
    val trayBounds = mutableStateMapOf<String, Rect>()
    var trayCard by mutableStateOf<Rect?>(null)
    var trayRow by mutableStateOf<Rect?>(null)
        private set
    var origin by mutableStateOf(Offset.Zero)
    var layerSize by mutableStateOf(Size.Zero)
    var rtl by mutableStateOf(false)
    var trayChangedAt = 0L
        private set

    fun updateTrayRow(rect: Rect?) {
        if (rect != trayRow) {
            trayChangedAt = System.nanoTime()
            trayRow = rect
        }
    }

    fun isFlying(uri: String): Boolean = flights.any { it.item.uri == uri }

    fun slotCenter(uri: String, slotPx: Float, gapPx: Float): Offset {
        val row = trayRow ?: trayCard
        val known = trayBounds[uri]
        if (row == null) {
            return known?.center ?: Offset(origin.x + layerSize.width / 2f, origin.y + layerSize.height - slotPx)
        }
        val half = slotPx / 2f
        val raw = known?.center ?: nextSlotCenter(uri, row, half, gapPx)
        return Offset(
            raw.x.fit(row.left + half, row.right - half),
            raw.y.fit(row.top + half, row.bottom - half)
        )
    }

    private fun nextSlotCenter(uri: String, row: Rect, half: Float, gapPx: Float): Offset {
        var edge: Float? = null
        for ((id, rect) in trayBounds) {
            if (id == uri) continue
            val current = edge
            edge = when {
                current == null -> if (rtl) rect.left else rect.right
                rtl -> minOf(current, rect.left)
                else -> maxOf(current, rect.right)
            }
        }
        val e = edge
        val x = when {
            e == null -> if (rtl) row.right - half else row.left + half
            rtl -> e - gapPx - half
            else -> e + gapPx + half
        }
        return Offset(x, row.center.y)
    }

    fun launch(item: MediaItem, from: Rect, bitmap: Bitmap?) {
        if (from.width <= 1f || from.height <= 1f) return
        if (isFlying(item.uri) || flights.size >= MAX_FLIGHTS) return
        val duration = if (flights.isEmpty()) FLIGHT_MS else FLIGHT_BUSY_MS
        flights.add(Flight(item, from, duration, bitmap))
    }

    fun finish(flight: Flight) {
        flights.remove(flight)
    }
}

internal val LocalPickerFlight = staticCompositionLocalOf<PickerFlight?> { null }

private const val FLIGHT_MS = 860
private const val FLIGHT_BUSY_MS = 600
private const val BUSY_STAGE_SCALE = 0.72f
private const val TRAY_SETTLE_TIMEOUT_MS = 650L
private const val TRAY_QUIET_NANOS = 50_000_000L
private const val LAND_WAIT_MS = 220L
private const val MAX_FLIGHTS = 12
private const val MID_SCALE = 0.62f
private val TrayThumbSize = 48.dp
private val TrayGap = 8.dp

private fun Float.fit(lo: Float, hi: Float): Float = if (lo <= hi) coerceIn(lo, hi) else (lo + hi) / 2f

private fun Modifier.flightLayout(from: Rect, origin: Offset, w: Int, h: Int): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints.fixed(w, h))
    layout(w, h) {
        placeable.place((from.left - origin.x).roundToInt(), (from.top - origin.y).roundToInt())
    }
}

private fun flightBounds(origin: Offset, size: Size, marginPx: Float): Rect? {
    if (size.width <= 0f || size.height <= 0f) return null
    val r = Rect(origin.x + marginPx, origin.y + marginPx, origin.x + size.width - marginPx, origin.y + size.height - marginPx)
    return if (r.width > 0f && r.height > 0f) r else null
}

private fun Offset.clampTo(r: Rect?): Offset =
    if (r == null) this else Offset(x.fit(r.left, r.right), y.fit(r.top, r.bottom))

@Composable
internal fun PickerFlightLayer(flight: PickerFlight, selectedUris: Set<String>, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    SideEffect { flight.rtl = rtl }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.fillMaxSize().onGloballyPositioned {
            flight.origin = it.positionInWindow()
            flight.layerSize = it.size.toSize()
        }) {
            flight.flights.forEach { f ->
                key(f) { FlyingThumb(flight, f, f.item.uri in selectedUris) }
            }
        }
    }
}

@Composable
private fun FlyingThumb(flight: PickerFlight, f: PickerFlight.Flight, stillSelected: Boolean) {
    val loaded by rememberMediaThumb(f.item, 256)
    val progress = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    FlightEffects(flight, f, stillSelected, progress, fade)
    FlightBody(flight, f, f.bitmap ?: loaded, progress, fade)
}

private suspend fun awaitTraySettled(flight: PickerFlight) {
    withTimeoutOrNull(TRAY_SETTLE_TIMEOUT_MS) {
        snapshotFlow { flight.trayRow }.first { it != null }
        while (System.nanoTime() - flight.trayChangedAt < TRAY_QUIET_NANOS) withFrameNanos { }
    }
}

@Composable
private fun FlightEffects(flight: PickerFlight, f: PickerFlight.Flight, stillSelected: Boolean, progress: Animatable<Float, *>, fade: Animatable<Float, *>) {
    val uri = f.item.uri
    val seenSelected = remember { BooleanArray(1) }
    LaunchedEffect(f) {
        awaitTraySettled(flight)
        progress.animateTo(1f, tween(f.durationMs, easing = LinearEasing))
        withTimeoutOrNull(LAND_WAIT_MS) { snapshotFlow { flight.trayBounds[uri] }.first { it != null } }
        flight.finish(f)
    }
    LaunchedEffect(stillSelected) {
        if (stillSelected) {
            seenSelected[0] = true
            fade.snapTo(1f)
        } else if (seenSelected[0]) {
            fade.animateTo(0f, tween(120))
            flight.finish(f)
        }
    }
}

@Composable
private fun FlightBody(flight: PickerFlight, f: PickerFlight.Flight, thumb: Bitmap?, progress: Animatable<Float, *>, fade: Animatable<Float, *>) {
    val uri = f.item.uri
    val density = LocalDensity.current
    val slotPx = with(density) { TrayThumbSize.toPx() }
    val gapPx = with(density) { TrayGap.toPx() }
    val fromCorner = with(density) { 12.dp.toPx() }
    val stageCorner = with(density) { 20.dp.toPx() }
    val toCorner = with(density) { 8.dp.toPx() }
    val maxShadow = with(density) { 24.dp.toPx() }
    val maxEdge = with(density) { 220.dp.toPx() }
    val edgeMarginPx = slotPx * MID_SCALE / 2f
    val placeholder = FrameTheme.colors.containerHigh
    val fromW = f.from.width.roundToInt().coerceAtLeast(1)
    val fromH = f.from.height.roundToInt().coerceAtLeast(1)
    val start = f.from.center
    val busy = f.durationMs == FLIGHT_BUSY_MS
    Box(
        Modifier
            .flightLayout(f.from, flight.origin, fromW, fromH)
            .graphicsLayer {
                val bounds = flightBounds(flight.origin, flight.layerSize, edgeMarginPx)
                val slot = flight.slotCenter(uri, slotPx, gapPx).clampTo(bounds)
                val stage = flightStage(flight.origin, flight.layerSize, f.from, maxEdge, if (busy) BUSY_STAGE_SCALE else 1f, stageCorner)
                val pose = flightPose(progress.value, f.from, fromCorner, stage, FlightEnd(slot, slotPx, toCorner))
                val sx = pose.width / fromW
                val sy = pose.height / fromH
                scaleX = sx
                scaleY = sy
                translationX = pose.center.x - start.x
                translationY = pose.center.y - start.y
                shape = RoundedCornerShape(pose.corner / sx.coerceAtLeast(0.01f))
                clip = true
                shadowElevation = maxShadow * pose.lift
                this.alpha = fade.value
            }
    ) {
        FlightFace(f, thumb, placeholder)
    }
}

@Composable
private fun FlightFace(f: PickerFlight.Flight, thumb: Bitmap?, placeholder: Color) {
    Box(Modifier.fillMaxSize().background(placeholder)) {
        if (thumb != null) {
            val image = remember(thumb) { thumb.asImageBitmap() }
            Image(image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            ThumbPlaceholder(icon = f.item.kind.icon())
        }
    }
}
