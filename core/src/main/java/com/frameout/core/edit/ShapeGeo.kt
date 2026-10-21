package com.frameout.core.edit

import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class ShapeGeo(m: ShapeMark, val w: Float, val h: Float) {
    val ref = min(w, h)
    val sw = (m.width * ref).coerceAtLeast(1f)
    val x1 = m.x1 * w
    val y1 = m.y1 * h
    val x2 = m.x2 * w
    val y2 = m.y2 * h
    val l = min(x1, x2)
    val t = min(y1, y2)
    val r = max(x1, x2)
    val b = max(y1, y2)
    val bw = r - l
    val bh = b - t
    val cx = (l + r) / 2f
    val cy = (t + b) / 2f
    val radius = min(bw, bh) / 2f
}

internal object ShapePaints {
    const val SOFT_FILL = 0.28f

    fun stroke(m: ShapeMark, sw: Float, dash: Boolean = m.dash && m.kind.supportsDash): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = m.color
        strokeWidth = sw
        strokeCap = if (dash) Paint.Cap.BUTT else Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        if (dash) pathEffect = DashPathEffect(floatArrayOf(sw * 2.4f, sw * 1.9f), 0f)
    }

    fun solid(color: Int, factor: Float = 1f): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = scaleAlpha(color, factor)
    }

    fun scaleAlpha(color: Int, factor: Float): Int =
        (color and 0x00FFFFFF) or ((Color.alpha(color) * factor.coerceIn(0f, 1f)).roundToInt() shl 24)

    fun onColor(color: Int): Int {
        val lum = 0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)
        return if (lum > 160f) 0xFF111111.toInt() else Color.WHITE
    }

    fun fillFactor(m: ShapeMark): Float = when (m.fill) {
        ShapeFill.None -> 0f
        ShapeFill.Soft -> SOFT_FILL
        ShapeFill.Solid -> 1f
    }
}
