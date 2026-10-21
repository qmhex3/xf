package com.frameout.designsystem.components

import android.graphics.RectF
import android.os.Build
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.frameout.designsystem.theme.FrameColorScheme
import com.frameout.designsystem.theme.FrameColors
import kotlin.math.min

internal data class CameraSpot(val cx: Float, val cy: Float, val d: Float, val onTop: Boolean = true)

private fun android.graphics.Rect.squareness(): Float {
    val w = width().toFloat().coerceAtLeast(1f)
    val h = height().toFloat().coerceAtLeast(1f)
    return if (w > h) h / w else w / h
}

internal fun readCameraSpot(view: View, minD: Float, maxD: Float): CameraSpot? {
    if (Build.VERSION.SDK_INT < 28) return null
    val cutout = view.rootWindowInsets?.displayCutout ?: return null
    val w = view.width
    val h = view.height
    if (w <= 0 || h <= 0) return null
    val rects = cutout.boundingRects.filter { !it.isEmpty && it.width() <= w * 0.6f && it.height() <= h * 0.35f }
    if (rects.isEmpty()) return null
    val edge = (minOf(w, h) * 0.04f).toInt().coerceAtLeast(4)
    val top = rects.filter { it.top <= edge }
    val pool = top.ifEmpty { rects }
    var r = pool.maxByOrNull { it.squareness() } ?: return null
    if (Build.VERSION.SDK_INT >= 31 && rects.size == 1) {
        val path = cutout.cutoutPath
        if (path != null) {
            val rf = RectF()
            path.computeBounds(rf, true)
            if (!rf.isEmpty && RectF.intersects(RectF(r), rf)) {
                r = android.graphics.Rect(rf.left.toInt(), rf.top.toInt(), rf.right.toInt(), rf.bottom.toInt())
            }
        }
    }
    val d = min(r.width(), r.height()).toFloat().coerceIn(minD, maxD)
    return CameraSpot(r.exactCenterX(), r.exactCenterY(), d, onTop = top.isNotEmpty())
}

@Composable
internal fun rememberCameraSpot(view: View, minD: Float, maxD: Float): CameraSpot? {
    var spot by remember(view, minD, maxD) { mutableStateOf(readCameraSpot(view, minD, maxD)) }
    DisposableEffect(view, minD, maxD) {
        val refresh = {
            val next = readCameraSpot(view, minD, maxD)
            if (next != spot) spot = next
        }
        val onLayout = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> refresh() }
        val onAttach = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = refresh()
            override fun onViewDetachedFromWindow(v: View) = Unit
        }
        view.addOnLayoutChangeListener(onLayout)
        view.addOnAttachStateChangeListener(onAttach)
        refresh()
        onDispose {
            view.removeOnLayoutChangeListener(onLayout)
            view.removeOnAttachStateChangeListener(onAttach)
        }
    }
    return spot
}

internal fun ToastKind.islandAccent(colors: FrameColorScheme): Color = when (this) {
    ToastKind.Error -> FrameColors.ToastError
    ToastKind.Success, ToastKind.Info -> colors.onDarkAccent
}

internal fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

internal fun ramp(x: Float, from: Float, to: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun contentK(appear: Float): Float = ramp(appear, 0.5f, 0.95f)
