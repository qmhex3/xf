package com.frameout.app.billing

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.sin

private val SkyTop = Color(0xFF1B1646)
private val SkyMid = Color(0xFFB4457E)
private val SkyLow = Color(0xFFFFA45C)
private val SkyHorizon = Color(0xFFFFC27A)
private val SunColor = Color(0xFFFFE2A0)
private val WindowLit = Color(0xFFFFD27D)
private val Skin = Color(0xFFC98B6B)
private val Ink = Color(0xFF241A3A)

internal fun DrawScope.drawSceneArt() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(0f to SkyTop, 0.5f to SkyMid, 0.78f to SkyLow, 1f to SkyHorizon))
    drawStars(w, h)
    drawSunGlow(w, h)
    drawRidge(w, h, 0.52f, Color(0xFF3A2A66), 0.9f, 0f)
    drawRidge(w, h, 0.60f, Color(0xFF241A4E), 1.4f, 0.4f)
    drawSkyline(w, h)
    drawRect(Brush.verticalGradient(listOf(Color(0xFF120E2E), Color(0xFF07061A)), startY = h * 0.78f, endY = h), Offset(0f, h * 0.78f), Size(w, h * 0.22f))
    drawPerson(w, h)
    drawPlate(w, h)
}

private fun DrawScope.drawStars(w: Float, h: Float) {
    for (i in 0 until 22) {
        val x = ((i * 0.618f) % 1f) * w
        val y = ((i * 0.382f + 0.13f) % 0.38f) * h
        drawCircle(Color.White.copy(alpha = 0.25f + (i % 4) * 0.12f), (0.6f + (i % 3) * 0.5f) * density, Offset(x, y))
    }
}

private fun DrawScope.drawSunGlow(w: Float, h: Float) {
    val c = Offset(w * 0.74f, h * 0.58f)
    val glow = Brush.radialGradient(listOf(SunColor.copy(alpha = 0.95f), SunColor.copy(alpha = 0.25f), Color.Transparent), c, h * 0.55f)
    drawCircle(glow, h * 0.55f, c)
    drawCircle(SunColor, h * 0.09f, c)
}

private fun DrawScope.drawRidge(w: Float, h: Float, base: Float, color: Color, freq: Float, phase: Float) {
    val path = Path().apply {
        moveTo(0f, h)
        var x = 0f
        while (x <= w + 4f) {
            val t = x / w
            lineTo(x, h * (base + 0.06f * sin(t * freq * 6.2f + phase * 6f) + 0.03f * sin(t * freq * 15f)))
            x += 4f
        }
        lineTo(w, h)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawSkyline(w: Float, h: Float) {
    val heights = floatArrayOf(0.22f, 0.30f, 0.18f, 0.36f, 0.26f, 0.20f, 0.33f, 0.24f)
    val bw = w / heights.size
    heights.forEachIndexed { i, bh ->
        val x = i * bw
        val top = h * (0.80f - bh)
        drawRect(Color(0xFF150F33), Offset(x, top), Size(bw * 0.92f, h * 0.8f - top))
        drawWindows(x, top, bw, h)
    }
}

private fun DrawScope.drawWindows(x: Float, top: Float, bw: Float, h: Float) {
    var wy = top + 6f
    while (wy < h * 0.76f) {
        var wx = x + 5f
        while (wx < x + bw * 0.86f) {
            if (((wx * 7 + wy * 3).toInt() % 5) < 2) drawRect(WindowLit.copy(alpha = 0.85f), Offset(wx, wy), Size(3f * density, 4f * density))
            wx += 8f * density
        }
        wy += 11f * density
    }
}

private fun DrawScope.drawPerson(w: Float, h: Float) {
    val c = Offset(w * SceneZones.face.x, h * SceneZones.face.y)
    val r = h * SceneZones.FACE_R
    drawRoundRect(Color(0xFF2C2350), Offset(c.x - r * 1.9f, c.y + r * 1.25f), Size(r * 3.8f, h), CornerRadius(r * 1.5f))
    drawRoundRect(Color(0xFFFF8F6B).copy(alpha = 0.55f), Offset(c.x + r * 0.9f, c.y + r * 1.3f), Size(r * 0.35f, h * 0.2f), CornerRadius(r))
    drawRect(Skin, Offset(c.x - r * 0.35f, c.y + r * 0.6f), Size(r * 0.7f, r * 0.9f))
    val lit = Offset(c.x - r * 0.3f, c.y - r * 0.3f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFF0B898), Skin), lit, r * 1.5f), r, c)
    drawArc(Color(0xFF1A1230), 180f, 190f, true, Offset(c.x - r * 1.05f, c.y - r * 1.15f), Size(r * 2.1f, r * 1.6f))
    drawCircle(Ink, r * 0.09f, Offset(c.x - r * 0.36f, c.y - r * 0.05f))
    drawCircle(Ink, r * 0.09f, Offset(c.x + r * 0.36f, c.y - r * 0.05f))
    drawArc(Color(0xFF7A3B3B), 20f, 140f, false, Offset(c.x - r * 0.3f, c.y + r * 0.2f), Size(r * 0.6f, r * 0.34f), style = Stroke(r * 0.07f))
}

private fun DrawScope.drawPlate(w: Float, h: Float) {
    val z = SceneZones.plate
    val tl = Offset(w * z.left, h * z.top)
    val sz = Size(w * z.width, h * z.height)
    drawRoundRect(Color(0xFF0B0B14), Offset(tl.x - 6f, tl.y - 6f), Size(sz.width + 12f, sz.height + 12f), CornerRadius(14f))
    drawRoundRect(Color(0xFFF4F1E4), tl, sz, CornerRadius(9f))
    drawRoundRect(Color(0xFF1C3FA8), tl, Size(sz.width * 0.14f, sz.height), CornerRadius(9f))
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF14141C.toInt()
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = sz.height * 0.58f
        textAlign = Paint.Align.CENTER
    }
    drawContext.canvas.nativeCanvas.drawText("48 \u00B7 ABC", tl.x + sz.width * 0.57f, tl.y + sz.height * 0.69f, paint)
}
