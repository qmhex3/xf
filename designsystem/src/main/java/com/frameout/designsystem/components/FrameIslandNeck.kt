package com.frameout.designsystem.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal class NeckGeo(
    val ax: Float,
    val ay: Float,
    val ar: Float,
    val left: Float,
    val top: Float,
    val width: Float,
    val corner: Float,
    val tension: Float
)

private const val NECK_REACH = 4f

internal fun neckPath(g: NeckGeo): Path? {
    val inset = min(g.corner, g.width / 2f)
    val px = g.ax.coerceIn(g.left + inset, g.left + g.width - inset)
    val rc = inset
    val sink = rc * 0.5f
    val py = g.top + sink
    val dx = px - g.ax
    val dy = py - g.ay
    val len = hypot(dx, dy)
    if (len < 1f) return null
    val gap = max(len - g.ar, 0f)
    val tension = g.tension * (1f - ramp(gap, 0f, g.ar * NECK_REACH))
    if (tension < 0.02f) return null
    val h0 = g.ar * 1.0f * ramp(tension, 0f, 0.3f)
    val hm = h0 * (0.25f + 0.6f * tension)
    val chord = sqrt(sink * (2f * rc - sink)).coerceAtLeast(1f)
    val h1 = min(h0 * 2.2f + 3f, chord)
    return bridge(Offset(g.ax, g.ay), Offset(px, py), Offset(-dy / len, dx / len), h0, hm, h1)
}

private fun bridge(a: Offset, b: Offset, n: Offset, h0: Float, hm: Float, h1: Float): Path {
    val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    val c = 2f * hm - (h0 + h1) / 2f
    return Path().apply {
        moveTo(a.x + n.x * h0, a.y + n.y * h0)
        quadraticTo(mid.x + n.x * c, mid.y + n.y * c, b.x + n.x * h1, b.y + n.y * h1)
        lineTo(b.x - n.x * h1, b.y - n.y * h1)
        quadraticTo(mid.x - n.x * c, mid.y - n.y * c, a.x - n.x * h0, a.y - n.y * h0)
        close()
    }
}

internal fun DrawScope.drawNeck(g: NeckGeo, color: Color) {
    neckPath(g)?.let { drawPath(it, color) }
}

internal fun DrawScope.drawCameraRing(spot: CameraSpot, rootPos: Offset, accent: Color, t: Float) {
    if (t >= 1f) return
    val c = Offset(spot.cx - rootPos.x, spot.cy - rootPos.y)
    val r = spot.d / 2f + 2.dp.toPx() + 14.dp.toPx() * t
    val a = 1f - t
    drawCircle(
        Brush.radialGradient(listOf(accent.copy(alpha = 0.32f * a), Color.Transparent), c, r * 2.1f),
        r * 2.1f, c
    )
    drawCircle(accent.copy(alpha = 0.9f * a), r, c, style = Stroke(width = 0.6.dp.toPx() + 1.8.dp.toPx() * a))
}
