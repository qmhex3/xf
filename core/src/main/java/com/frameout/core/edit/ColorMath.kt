package com.frameout.core.edit

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class Hsv(val h: Float, val s: Float, val v: Float)

object ColorMath {
    private const val OPAQUE = 0xFF000000.toInt()

    fun toArgb(hsv: Hsv): Int {
        val h = ((hsv.h % 360f) + 360f) % 360f
        val s = hsv.s.coerceIn(0f, 1f)
        val v = hsv.v.coerceIn(0f, 1f)
        val c = v * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r, g, b) = when ((h / 60f).toInt().coerceIn(0, 5)) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return pack(r + m, g + m, b + m)
    }

    fun fromArgb(argb: Int): Hsv {
        val r = channel(argb shr 16)
        val g = channel(argb shr 8)
        val b = channel(argb)
        val top = max(r, max(g, b))
        val delta = top - min(r, min(g, b))
        val hue = when {
            delta == 0f -> 0f
            top == r -> 60f * (((g - b) / delta + 6f) % 6f)
            top == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }
        return Hsv(hue, if (top == 0f) 0f else delta / top, top)
    }

    fun hex(argb: Int): String = "#%06X".format(java.util.Locale.ROOT, argb and 0xFFFFFF)

    private fun channel(shifted: Int): Float = (shifted and 0xFF) / 255f

    private fun pack(r: Float, g: Float, b: Float): Int =
        OPAQUE or (to255(r) shl 16) or (to255(g) shl 8) or to255(b)

    private fun to255(v: Float): Int = (v.coerceIn(0f, 1f) * 255f).roundToInt()
}
