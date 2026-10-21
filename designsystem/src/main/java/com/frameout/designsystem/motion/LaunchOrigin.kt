package com.frameout.designsystem.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.min

@Immutable
data class OriginSnap(val center: Offset, val radius: Float, val color: Color) {
    companion object {
        val None = OriginSnap(Offset.Zero, 0f, Color.Unspecified)
    }
}

class OriginAnchor {
    internal var coordinates: LayoutCoordinates? = null
}

@Composable
fun rememberOriginAnchor(): OriginAnchor = remember { OriginAnchor() }

fun Modifier.anchorOrigin(anchor: OriginAnchor): Modifier = onGloballyPositioned { anchor.coordinates = it }

object LaunchOrigin {
    private var tracked: LayoutCoordinates? = null
    private var stampedAt = 0L
    private var snap = OriginSnap.None
    private const val FRESH_NANOS = 1_500_000_000L

    internal fun track(coordinates: LayoutCoordinates) {
        tracked = coordinates
    }

    fun mark(tint: Color) = stamp(tracked, tint)

    fun mark(anchor: OriginAnchor, tint: Color) = stamp(anchor.coordinates, tint)

    private fun stamp(coordinates: LayoutCoordinates?, tint: Color) {
        val c = coordinates?.takeIf { it.isAttached } ?: return
        val bounds = c.boundsInWindow()
        snap = OriginSnap(bounds.center, min(bounds.width, bounds.height) / 2f, tint)
        stampedAt = System.nanoTime()
    }

    fun snapshot(): OriginSnap = snap

    fun fresh(): Boolean = stampedAt != 0L && System.nanoTime() - stampedAt < FRESH_NANOS
}

fun Modifier.trackLaunchOrigin(): Modifier = onGloballyPositioned { LaunchOrigin.track(it) }
