package com.frameout.app

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.max

object CellOrigin {
    private var bounds = Rect.Zero
    private var stampedAt = 0L
    private const val FRESH_NANOS = 1_500_000_000L

    fun mark(coordinates: LayoutCoordinates?) {
        val c = coordinates?.takeIf { it.isAttached } ?: return
        bounds = c.boundsInWindow()
        stampedAt = System.nanoTime()
    }

    fun rect(): Rect = bounds

    fun fresh(): Boolean = stampedAt != 0L && bounds.width > 1f && bounds.height > 1f && System.nanoTime() - stampedAt < FRESH_NANOS
}

class ViewerRevealState {
    var active by mutableStateOf(false)
    var opening by mutableStateOf(true)
    var target by mutableStateOf<AppRoute?>(null)
    var cell by mutableStateOf(Rect.Zero)
    var allowed = true
    private var revealed: AppRoute? = null

    fun onNavigate(from: AppRoute?, to: AppRoute) {
        val ok = allowed && to is AppRoute.Viewer && (from is AppRoute.Frames || from is AppRoute.Extracting) && CellOrigin.fresh()
        active = ok
        opening = true
        revealed = if (ok) to else null
        if (ok) {
            target = to
            cell = CellOrigin.rect()
        }
    }

    fun onBack(from: AppRoute?) {
        active = allowed && from != null && from == revealed
        opening = false
        if (active) target = from
        revealed = null
    }

    fun reset() {
        active = false
        revealed = null
    }
}

private val OutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val InOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private const val OPEN_MS = 480
private const val CLOSE_MS = 340
private const val FINISHED = 0.999f

@Composable
internal fun AnimatedContentScope.ViewerRevealHost(route: AppRoute, state: ViewerRevealState, content: @Composable () -> Unit) {
    val progress by transition.animateFloat(
        transitionSpec = { if (targetState == EnterExitState.Visible) tween(OPEN_MS, easing = OutQuint) else tween(CLOSE_MS, easing = InOut) },
        label = "viewerReveal"
    ) { if (it == EnterExitState.Visible || !state.active) 1f else 0f }
    var pos by remember { mutableStateOf(Offset.Zero) }
    val mod = when {
        !state.active -> Modifier
        route == state.target -> Modifier.cellExpand({ progress }) { state.cell.translate(-pos) }
        else -> Modifier.cellUnderlay { progress }
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned { pos = it.positionInWindow() }) {
        Box(mod.fillMaxSize()) { content() }
    }
}

private class CellShape(private val w: Float, private val h: Float, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val left = (size.width - w) / 2f
        val top = (size.height - h) / 2f
        return Outline.Rounded(RoundRect(left, top, left + w, top + h, radius, radius))
    }
}

private fun smooth(x: Float, from: Float, to: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun Modifier.cellExpand(progress: () -> Float, cell: () -> Rect): Modifier = graphicsLayer {
    val p = progress()
    val c = cell()
    if (p >= FINISHED || size.width <= 0f || size.height <= 0f || c.width <= 1f) return@graphicsLayer
    val k = lerp(max(c.width / size.width, c.height / size.height), 1f, p)
    scaleX = k
    scaleY = k
    translationX = (c.center.x - size.width / 2f) * (1f - p)
    translationY = (c.center.y - size.height / 2f) * (1f - p)
    clip = true
    shape = CellShape(lerp(c.width, size.width, p) / k, lerp(c.height, size.height, p) / k, lerp(10.dp.toPx(), 0f, p) / k)
    alpha = 0.3f + 0.7f * smooth(p, 0f, 0.3f)
}

private fun Modifier.cellUnderlay(progress: () -> Float): Modifier = graphicsLayer {
    val p = progress()
    val s = 0.95f + 0.05f * p
    scaleX = s
    scaleY = s
    alpha = 0.4f + 0.6f * p
}
