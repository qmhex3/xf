package com.frameout.app.settings

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val CARDS = 5
private val PICKED = setOf(1, 3)

internal fun DrawScope.drawContent(pick: Color, dim: Color, ink: Color) {
    val gap = 6.dp.toPx()
    val w = minOf((size.width - 20.dp.toPx() - gap * (CARDS - 1)) / CARDS, 56.dp.toPx())
    val h = 36.dp.toPx()
    val startX = (size.width - (w * CARDS + gap * (CARDS - 1))) / 2f
    val y = (size.height - h) / 2f
    for (i in 0 until CARDS) {
        val x = startX + i * (w + gap)
        if (i in PICKED) pickedCard(x, y, w, h, pick, ink) else idleCard(x, y, w, h, dim, ink)
    }
}

private fun DrawScope.idleCard(x: Float, y: Float, w: Float, h: Float, dim: Color, ink: Color) {
    drawRoundRect(dim.copy(alpha = 0.16f), Offset(x, y), Size(w, h), CornerRadius(5.dp.toPx()))
    drawLine(ink.copy(alpha = 0.45f), Offset(x + w * 0.3f, y + h * 0.35f), Offset(x + w * 0.7f, y + h * 0.65f), 1.5.dp.toPx())
    drawLine(ink.copy(alpha = 0.45f), Offset(x + w * 0.7f, y + h * 0.35f), Offset(x + w * 0.3f, y + h * 0.65f), 1.5.dp.toPx())
}

private fun DrawScope.pickedCard(x: Float, y: Float, w: Float, h: Float, pick: Color, ink: Color) {
    drawRoundRect(pick.copy(alpha = 0.18f), Offset(x, y), Size(w, h), CornerRadius(5.dp.toPx()))
    drawRoundRect(pick, Offset(x, y), Size(w, h), CornerRadius(5.dp.toPx()), style = Stroke(1.5.dp.toPx()))
    val body = Size(w * 0.5f, h * 0.22f)
    drawRoundRect(pick, Offset(x + w * 0.25f, y + h * 0.4f), body, CornerRadius(3.dp.toPx()))
    drawRoundRect(pick, Offset(x + w * 0.34f, y + h * 0.26f), Size(w * 0.32f, h * 0.18f), CornerRadius(3.dp.toPx()))
    drawCircle(ink.copy(alpha = 0.9f), 2.dp.toPx(), Offset(x + w * 0.34f, y + h * 0.66f))
    drawCircle(ink.copy(alpha = 0.9f), 2.dp.toPx(), Offset(x + w * 0.66f, y + h * 0.66f))
}
