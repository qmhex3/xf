package com.frameout.core.edit

import kotlin.math.max
import kotlin.math.min

data class Transform(val scale: Float, val x: Float, val y: Float) {
    companion object {
        val Identity = Transform(1f, 0f, 0f)
    }
}

data class ViewGeometry(val imageW: Int, val imageH: Int, val viewW: Float, val viewH: Float) {
    val fit: Float = if (imageW <= 0 || imageH <= 0 || viewW <= 0f || viewH <= 0f) 1f else min(viewW / imageW, viewH / imageH)
    val maxScale: Float = (MAX_SCREEN_PX_PER_PIXEL / fit).coerceIn(MIN_MAX_SCALE, ABS_MAX_SCALE)

    fun pixelSize(t: Transform): Float = fit * t.scale

    fun clamp(t: Transform): Transform {
        val s = t.scale.coerceIn(1f, maxScale)
        return Transform(s, clampAxis(t.x, imageW * fit * s, viewW), clampAxis(t.y, imageH * fit * s, viewH))
    }

    fun zoomed(t: Transform, factor: Float, focalX: Float, focalY: Float): Transform {
        val s = (t.scale * factor).coerceIn(1f, maxScale)
        val k = s / t.scale
        return clamp(Transform(s, focalX - (focalX - t.x) * k, focalY - (focalY - t.y) * k))
    }

    fun gesture(t: Transform, factor: Float, focalX: Float, focalY: Float, panX: Float, panY: Float): Transform {
        val z = zoomed(t, factor, focalX, focalY)
        return clamp(z.copy(x = z.x + panX, y = z.y + panY))
    }

    fun toNormX(screenX: Float, t: Transform): Float = toImageX(screenX, t) / imageW.coerceAtLeast(1)

    fun toNormY(screenY: Float, t: Transform): Float = toImageY(screenY, t) / imageH.coerceAtLeast(1)

    fun toScreenX(normX: Float, t: Transform): Float = (normX * imageW - imageW / 2f) * fit * t.scale + t.x

    fun toScreenY(normY: Float, t: Transform): Float = (normY * imageH - imageH / 2f) * fit * t.scale + t.y

    fun toImageX(screenX: Float, t: Transform): Float = (screenX - t.x) / (fit * t.scale) + imageW / 2f

    fun toImageY(screenY: Float, t: Transform): Float = (screenY - t.y) / (fit * t.scale) + imageH / 2f

    fun actualPixels(): Float = (1f / fit).coerceIn(1f, maxScale)

    private fun clampAxis(offset: Float, shown: Float, view: Float): Float {
        val limit = max(0f, (shown - view) / 2f)
        return offset.coerceIn(-limit, limit)
    }

    companion object {
        const val MAX_SCREEN_PX_PER_PIXEL = 48f
        const val MIN_MAX_SCALE = 8f
        const val ABS_MAX_SCALE = 512f
        const val GRID_MIN_CELL = 7f
    }
}
