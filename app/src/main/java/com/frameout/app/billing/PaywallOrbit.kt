package com.frameout.app.billing

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private const val ORBIT_STEP = 2.094f

internal class OrbitCard(val kind: Int, val at: Offset, val z: Float)

internal fun DrawScope.orbitCards(center: Offset, t: Float): List<OrbitCard> =
    (0 until 3).map { k ->
        val a = t * 0.55f + k * ORBIT_STEP
        val at = Offset(center.x + cos(a) * size.width * 0.34f, center.y + sin(a) * 22.dp.toPx() + (k - 1) * 3.dp.toPx())
        OrbitCard(k, at, sin(a))
    }.sortedBy { it.z }

internal fun DrawScope.drawFrameCard(card: OrbitCard, p: HeroPalette, alpha: Float) {
    val scale = 0.72f + 0.1f * card.z
    val unit = scale * 1.dp.toPx()
    val w = 46f * unit
    val h = 36f * unit
    val topLeft = Offset(card.at.x - w / 2f, card.at.y - h / 2f)
    val fill = Brush.linearGradient(listOf(p.c.copy(alpha = 0.9f * alpha), p.b.copy(alpha = 0.9f * alpha)), topLeft, Offset(topLeft.x + w, topLeft.y + h))
    drawRoundRect(fill, topLeft, Size(w, h), CornerRadius(9f * unit))
    drawRoundRect(Color.White.copy(alpha = 0.55f * alpha), topLeft, Size(w, h), CornerRadius(9f * unit), style = Stroke(1.2f * unit))
    drawGlyph(card.kind, card.at, unit, Color.White.copy(alpha = alpha))
}

private fun DrawScope.drawGlyph(kind: Int, at: Offset, u: Float, tint: Color) {
    val stroke = Stroke(1.8f * u, cap = StrokeCap.Round)
    when (kind) {
        0 -> {
            drawCircle(tint, 6f * u, Offset(at.x - 3f * u, at.y), style = stroke)
            drawCircle(tint, 6f * u, Offset(at.x + 4f * u, at.y), style = stroke)
        }
        1 -> {
            val tip = Offset(at.x + 8f * u, at.y - 7f * u)
            drawLine(tint, Offset(at.x - 9f * u, at.y + 7f * u), tip, 1.8f * u, StrokeCap.Round)
            drawLine(tint, tip, Offset(tip.x - 7f * u, tip.y), 1.8f * u, StrokeCap.Round)
            drawLine(tint, tip, Offset(tip.x, tip.y + 7f * u), 1.8f * u, StrokeCap.Round)
        }
        else -> for (row in 0..1) for (col in 0..1) {
            val origin = Offset(at.x + (-9f + col * 10f) * u, at.y + (-7f + row * 8f) * u)
            drawRoundRect(tint, origin, Size(8f * u, 6f * u), CornerRadius(1.2f * u), style = stroke)
        }
    }
}
