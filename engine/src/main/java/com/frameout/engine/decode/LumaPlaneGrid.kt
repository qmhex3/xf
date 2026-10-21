package com.frameout.engine.decode

import android.graphics.Rect
import android.media.Image
import com.frameout.core.quality.LumaGrid

internal object LumaPlaneGrid {
    fun of(plane: Image.Plane, crop: Rect, turn: Int, flip: Boolean, wide: Boolean, limited: Boolean): LumaGrid {
        val cw = crop.width()
        val ch = crop.height()
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        return LumaGrid { ox, oy ->
            val fx: Int
            val fy: Int
            when (turn) {
                90 -> { fx = oy; fy = ch - 1 - ox }
                180 -> { fx = cw - 1 - ox; fy = ch - 1 - oy }
                270 -> { fx = cw - 1 - oy; fy = ox }
                else -> { fx = ox; fy = oy }
            }
            val sx = (if (flip) cw - 1 - fx else fx).coerceIn(0, cw - 1)
            val sy = fy.coerceIn(0, ch - 1)
            val pos = (crop.top + sy) * rowStride + (crop.left + sx) * pixelStride + if (wide) 1 else 0
            val y = if (pos in 0 until buffer.limit()) buffer.get(pos).toInt() and 255 else 0
            if (limited) ((y - 16) * 255 / 219).coerceIn(0, 255) else y
        }
    }
}
