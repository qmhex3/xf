package com.frameout.app.trim

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

data class ZoomState(val scale: Float = 1f, val offset: Offset = Offset.Zero) {
    val zoomed: Boolean get() = scale > MIN_SCALE

    fun transform(centroid: Offset, pan: Offset, zoom: Float, size: Size): ZoomState {
        val next = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        if (next <= MIN_SCALE) return ZoomState()
        val center = Offset(size.width / 2f, size.height / 2f)
        val ratio = next / scale
        val moved = centroid + pan - center - (centroid - center - offset) * ratio
        return ZoomState(next, clamp(moved, next, size))
    }

    fun relaxed(t: Float): ZoomState {
        val k = t.coerceIn(0f, 1f)
        if (k >= 1f) return ZoomState()
        return ZoomState(1f + (scale - 1f) * (1f - k), offset * (1f - k))
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 8f

        fun clamp(offset: Offset, scale: Float, size: Size): Offset {
            val limitX = size.width * (scale - 1f) / 2f
            val limitY = size.height * (scale - 1f) / 2f
            return Offset(offset.x.coerceIn(-limitX, limitX), offset.y.coerceIn(-limitY, limitY))
        }
    }
}
