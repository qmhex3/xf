package com.frameout.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun IslandPill(
    state: IslandToastState,
    accent: Color,
    reduce: Boolean,
    dismissable: Boolean,
    originD: Float,
    anchored: Boolean,
    appear: () -> Float,
    expand: () -> Float,
    timer: () -> Float,
    checkP: () -> Float,
    pop: () -> Float,
    chrome: () -> Float,
    drag: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val shape = remember(density.density) { PillShape(with(density) { EXPANDED_MAX_CORNER.toPx() }) }
    val dismissPx = with(density) { 36.dp.toPx() }
    val hasExpanded = state.expandable

    Layout(
        content = {
            Box(
                Modifier.graphicsLayer {
                    val k = contentK(appear())
                    val e = if (hasExpanded) expand() else 0f
                    alpha = k * (1f - ramp(e, 0f, 0.45f))
                    val sc = 0.88f + 0.12f * k
                    scaleX = sc; scaleY = sc
                }.then(if (hasExpanded) Modifier.clearAndSetSemantics { } else Modifier)
            ) { CompactLayer(state, accent, checkP, pop, liveText = !hasExpanded) }
            if (hasExpanded) {
                Box(
                    Modifier.graphicsLayer {
                        val k = contentK(appear())
                        val e = ramp(expand(), 0.35f, 0.85f)
                        alpha = k * e
                        val sc = 0.94f + 0.06f * e
                        scaleX = sc; scaleY = sc
                    }
                ) { ExpandedLayer(state, accent, checkP, pop) }
            }
        },
        modifier = Modifier
            .toastFeedback(if (state.busy) ToastKind.Info else state.kind, state.title)
            .pointerInput(dismissable) {
                if (dismissable) detectTapGestures { onDismiss() }
            }
            .pointerInput(dismissable) {
                if (!dismissable) return@pointerInput
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (drag.value < -dismissPx) onDismiss() else scope.launch { drag.animateTo(0f, spring()) }
                    },
                    onDragCancel = { scope.launch { drag.animateTo(0f, spring()) } },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        scope.launch { drag.snapTo((drag.value + dy * 0.8f).coerceIn(-320f, 24f)) }
                    }
                )
            }
            .graphicsLayer {
                val p = appear()
                translationY = drag.value
                alpha = if (reduce) p else if (anchored) 1f else (p / 0.14f).coerceIn(0f, 1f)
                shadowElevation = 14.dp.toPx() * ramp(p, 0.6f, 1f) * chrome()
                this.shape = shape
                clip = false
                ambientShadowColor = accent.copy(alpha = 0.20f)
                spotShadowColor = accent.copy(alpha = 0.55f)
            }
            .clip(shape)
            .background(Color.Black)
            .drawWithContent {
                val ch = chrome()
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.20f * ch), Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.7f
                    )
                )
                drawContent()
                drawOutline(
                    shape.createOutline(size, layoutDirection, this),
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), accent.copy(alpha = 0.22f))),
                    alpha = ch,
                    style = Stroke(2.dp.toPx())
                )
                val e = if (hasExpanded) expand() else 0f
                if (e > 0.02f) {
                    val bar = 2.dp.toPx()
                    drawRect(
                        accent.copy(alpha = 0.85f * e),
                        topLeft = Offset(0f, size.height - bar),
                        size = Size(size.width * timer().coerceIn(0f, 1f), bar)
                    )
                }
            }
    ) { ms, c ->
        val a = ms[0].measure(Constraints(maxWidth = min(c.maxWidth, (if (hasExpanded) COMPACT_MAX_W else EXPANDED_W).roundToPx())))
        val bW = min(c.maxWidth, EXPANDED_W.roundToPx())
        val b = if (ms.size > 1) ms[1].measure(Constraints(minWidth = bW, maxWidth = bW)) else null
        val p = appear()
        val e = if (b != null) expand() else 0f
        val st = if (anchored) sin(PI.toFloat() * p.coerceIn(0f, 1f)) else 0f
        val wc = lerpF(originD, a.width.toFloat(), if (anchored) ramp(p, 0.2f, 1f) else p)
        val hc = lerpF(originD, a.height.toFloat(), p) + originD * 0.5f * st * (1f - (if (b != null) expand() else 0f))
        val w = if (b != null) lerpF(wc, b.width.toFloat(), e) else wc
        val h = if (b != null) lerpF(hc, b.height.toFloat(), e) else hc
        val wi = max(w.roundToInt(), 1)
        val hi = max(h.roundToInt(), 1)
        layout(wi, hi) {
            a.place((wi - a.width) / 2, (hi - a.height) / 2)
            b?.place((wi - b.width) / 2, (hi - b.height) / 2)
        }
    }
}

internal class PillShape(private val maxCorner: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = min(size.height / 2f, maxCorner)
        return Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r)))
    }
}
