package com.frameout.app.viewer

import com.frameout.core.edit.CropRect
import kotlin.math.max
import kotlin.math.min

internal object GeometryMath {
    fun swaps(rotation: Int): Boolean = normalize(rotation) % 180 != 0

    fun normalize(rotation: Int): Int = ((rotation % 360) + 360) % 360

    fun sourceToDisplay(r: CropRect, rotation: Int, flipH: Boolean, flipV: Boolean): CropRect {
        val a = forward(r.l, r.t, rotation, flipH, flipV)
        val b = forward(r.r, r.b, rotation, flipH, flipV)
        return CropRect(min(a[0], b[0]), min(a[1], b[1]), max(a[0], b[0]), max(a[1], b[1]))
    }

    fun displayToSource(r: CropRect, rotation: Int, flipH: Boolean, flipV: Boolean): CropRect {
        val a = inverse(r.l, r.t, rotation, flipH, flipV)
        val b = inverse(r.r, r.b, rotation, flipH, flipV)
        return CropRect(min(a[0], b[0]), min(a[1], b[1]), max(a[0], b[0]), max(a[1], b[1]))
    }

    fun toggleDisplayFlipH(rotation: Int, flipH: Boolean, flipV: Boolean): Pair<Boolean, Boolean> =
        if (swaps(rotation)) flipH to !flipV else !flipH to flipV

    fun toggleDisplayFlipV(rotation: Int, flipH: Boolean, flipV: Boolean): Pair<Boolean, Boolean> =
        if (swaps(rotation)) !flipH to flipV else flipH to !flipV

    private fun forward(u: Float, v: Float, rotation: Int, flipH: Boolean, flipV: Boolean): FloatArray {
        val fu = if (flipH) 1f - u else u
        val fv = if (flipV) 1f - v else v
        return when (normalize(rotation)) {
            90 -> floatArrayOf(1f - fv, fu)
            180 -> floatArrayOf(1f - fu, 1f - fv)
            270 -> floatArrayOf(fv, 1f - fu)
            else -> floatArrayOf(fu, fv)
        }
    }

    private fun inverse(x: Float, y: Float, rotation: Int, flipH: Boolean, flipV: Boolean): FloatArray {
        val fu: Float
        val fv: Float
        when (normalize(rotation)) {
            90 -> { fu = y; fv = 1f - x }
            180 -> { fu = 1f - x; fv = 1f - y }
            270 -> { fu = 1f - y; fv = x }
            else -> { fu = x; fv = y }
        }
        return floatArrayOf(if (flipH) 1f - fu else fu, if (flipV) 1f - fv else fv)
    }
}
