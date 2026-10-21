package com.frameout.app.billing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val SPARKS = 30
private const val BOLT_DP = 150f

internal fun DrawScope.drawBoltScene(t: Float, pulse: Float, colors: BoltColors) {
    val center = Offset(size.width / 2f, size.height * 0.27f)
    val reach = minOf(size.width, size.height) * 0.55f
    drawHalo(center, reach, t, pulse, colors)
    drawFlash(center, reach, t, colors)
    drawRings(center, reach, t, colors)
    drawSparks(center, reach, t, colors)
    drawBolt(center, t, pulse, colors)
}

private fun DrawScope.drawHalo(c: Offset, reach: Float, t: Float, pulse: Float, colors: BoltColors) {
    val k = seg(t, 0.2f, 0.6f) * (0.28f + 0.14f * pulse)
    if (k <= 0f) return
    val brush = Brush.radialGradient(listOf(colors.second.copy(alpha = k), colors.first.copy(alpha = k * 0.35f), Color.Transparent), c, reach)
    drawCircle(brush, reach, c)
}

private fun DrawScope.drawFlash(c: Offset, reach: Float, t: Float, colors: BoltColors) {
    val f = seg(t, 0.02f, 0.07f) * (1f - seg(t, 0.07f, 0.30f))
    if (f <= 0f) return
    val r = reach * 1.4f
    val brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f * f), colors.second.copy(alpha = 0.4f * f), Color.Transparent), c, r)
    drawCircle(brush, r, c)
}

private fun DrawScope.drawRings(c: Offset, reach: Float, t: Float, colors: BoltColors) {
    for (k in 0..1) {
        val p = ((t - (0.08f + 0.12f * k)) / 0.6f).coerceIn(0f, 1f)
        if (p <= 0f || p >= 1f) continue
        val r = reach * easeOut(p) * (1f - 0.15f * k)
        val width = 3.dp.toPx() * (1f - p) + 0.5f
        drawCircle(colors.at(k).copy(alpha = (1f - p) * 0.55f), r, c, style = Stroke(width))
    }
}

private fun DrawScope.drawSparks(c: Offset, reach: Float, t: Float, colors: BoltColors) {
    for (i in 0 until SPARKS) {
        val start = 0.10f + unitHash(i * 1.7f) * 0.10f
        val p = ((t - start) / 0.55f).coerceIn(0f, 1f)
        if (p <= 0f || p >= 1f) continue
        val angle = i * 2.39996f
        val dir = Offset(cos(angle), sin(angle))
        val pos = c + dir * (reach * (0.45f + 0.55f * unitHash(i * 3.1f + 4f)) * easeOut(p))
        val color = colors.at(i).copy(alpha = 1f - p)
        drawLine(color, pos - dir * (14.dp.toPx() * (1f - p)), pos, 1.6.dp.toPx(), StrokeCap.Round)
        drawCircle(color, (2.6f * (1f - p) + 0.6f).dp.toPx(), pos)
    }
}

private fun DrawScope.drawBolt(c: Offset, t: Float, pulse: Float, colors: BoltColors) {
    val pop = 1f + 0.08f * sin(PI.toFloat() * seg(t, 0.18f, 0.45f)) + 0.015f * pulse
    val scale = BOLT_DP.dp.toPx() / BoltGeo.HEIGHT * pop
    val path = BoltGeo.path(scale, c)
    val fill = seg(t, 0.18f, 0.34f)
    val round = { w: Float -> Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    drawPath(path, colors.second.copy(alpha = (0.20f + 0.12f * pulse) * fill), style = round(26.dp.toPx()))
    drawPath(path, colors.first.copy(alpha = 0.35f * fill), style = round(12.dp.toPx()))
    if (fill < 1f) drawPath(path.trimmed(seg(t, 0.04f, 0.26f)), Color.White, style = round(2.dp.toPx()))
    val brush = Brush.linearGradient(listOf(colors.first, colors.second, colors.third), Offset(c.x - 50f * scale, c.y - 70f * scale), Offset(c.x + 50f * scale, c.y + 70f * scale))
    drawPath(path, brush, alpha = fill)
    drawPath(path, Color.White.copy(alpha = 0.85f * fill), style = round(1.2.dp.toPx()))
}
