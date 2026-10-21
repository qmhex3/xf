package com.frameout.app

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.frameout.designsystem.motion.LaunchOrigin
import com.frameout.designsystem.motion.OriginSnap
import kotlin.math.hypot
import kotlin.math.max

class FabRevealState {
    var active by mutableStateOf(false)
    var opening by mutableStateOf(true)
    var target by mutableStateOf<AppRoute?>(null)
    var origin by mutableStateOf(OriginSnap.None)
    var allowed = true
    private val revealed = mutableMapOf<AppRoute, OriginSnap>()

    fun onNavigate(to: AppRoute) = open(to)

    fun onReplace(to: AppRoute) {
        revealed.clear()
        open(to)
    }

    fun onBack(from: AppRoute?) {
        val snap = from?.let { revealed.remove(it) }
        active = allowed && snap != null
        opening = false
        if (snap != null) {
            target = from
            origin = snap
        }
    }

    fun reset() {
        active = false
        revealed.clear()
    }

    private fun open(to: AppRoute) {
        val ok = allowed && (to is AppRoute.Picker || to is AppRoute.Settings || to is AppRoute.Library) && LaunchOrigin.fresh()
        active = ok
        opening = true
        if (ok) {
            target = to
            origin = LaunchOrigin.snapshot()
            revealed[to] = origin
        }
    }
}

private val OutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val InOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private const val OPEN_MS = 560
private const val CLOSE_MS = 400

internal fun fabTransition(opening: Boolean): ContentTransform = ContentTransform(
    fadeIn(tween(1)),
    fadeOut(tween(if (opening) OPEN_MS else CLOSE_MS), targetAlpha = 0.999f),
    if (opening) 1f else -1f,
    SizeTransform(clip = false)
)

@Composable
internal fun AnimatedContentScope.FabRevealHost(route: AppRoute, fab: FabRevealState, content: @Composable () -> Unit) {
    val progress by transition.animateFloat(
        transitionSpec = { if (targetState == EnterExitState.Visible) tween(OPEN_MS, easing = OutQuint) else tween(CLOSE_MS, easing = InOut) },
        label = "fabReveal"
    ) { if (it == EnterExitState.Visible || !fab.active) 1f else 0f }
    var pos by remember { mutableStateOf(Offset.Zero) }
    val origin = { fab.origin.center - pos }
    Box(Modifier.fillMaxSize().onGloballyPositioned { pos = it.positionInWindow() }) {
        if (route == fab.target) {
            ScreenReveal({ progress }, origin, { fab.origin }, content)
        } else {
            Box(Modifier.fillMaxSize().underlay({ progress }, origin)) { content() }
        }
    }
}

@Composable
private fun ScreenReveal(progress: () -> Float, origin: () -> Offset, snap: () -> OriginSnap, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().drawWithContent { drawContent(); drawRing(progress(), origin(), snap()) }) {
        Box(Modifier.fillMaxSize().discClip(progress, origin, snap).drawWithContent { drawContent(); drawInk(progress(), snap().color) }) {
            Box(Modifier.fillMaxSize().contentRise(progress, origin)) { content() }
        }
    }
}

private class DiscShape(private val center: Offset, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Path().apply { addOval(Rect(center, radius)) })
}

private fun smooth(x: Float, from: Float, to: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun radiusAt(size: Size, c: Offset, f: Float, r0: Float): Float {
    val reach = hypot(max(c.x, size.width - c.x), max(c.y, size.height - c.y))
    return lerp(r0, reach, f.coerceIn(0f, 1f))
}

private fun Modifier.discClip(progress: () -> Float, origin: () -> Offset, snap: () -> OriginSnap): Modifier = graphicsLayer {
    val f = progress()
    if (f >= 0.999f) return@graphicsLayer
    clip = true
    shape = DiscShape(origin(), radiusAt(size, origin(), f, snap().radius))
}

private fun Modifier.contentRise(progress: () -> Float, origin: () -> Offset): Modifier = graphicsLayer {
    val f = progress()
    val c = origin()
    if (size.width > 0f && size.height > 0f) transformOrigin = TransformOrigin(c.x / size.width, c.y / size.height)
    val s = 0.9f + 0.1f * f
    scaleX = s
    scaleY = s
    alpha = smooth(f, 0.3f, 0.7f)
}

private fun Modifier.underlay(progress: () -> Float, origin: () -> Offset): Modifier = graphicsLayer {
    val f = progress()
    val c = origin()
    if (size.width > 0f && size.height > 0f) transformOrigin = TransformOrigin(c.x / size.width, c.y / size.height)
    val s = 0.94f + 0.06f * f
    scaleX = s
    scaleY = s
    alpha = 0.45f + 0.55f * f
}

private fun DrawScope.drawInk(f: Float, color: Color) {
    if (f >= 0.62f || color == Color.Unspecified) return
    drawRect(color.copy(alpha = 1f - smooth(f, 0.22f, 0.62f)))
}

private fun DrawScope.drawRing(f: Float, c: Offset, snap: OriginSnap) {
    if (f >= 0.999f || snap.color == Color.Unspecified) return
    val r = radiusAt(size, c, f, snap.radius)
    val fade = 1f - f
    drawCircle(snap.color.copy(alpha = 0.22f * fade), r + 10.dp.toPx() * fade, c, style = Stroke(width = 14.dp.toPx() * fade))
    drawCircle(snap.color.copy(alpha = 0.6f * fade), r, c, style = Stroke(width = 1.5.dp.toPx()))
}
