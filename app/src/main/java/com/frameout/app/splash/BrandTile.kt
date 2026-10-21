package com.frameout.app.splash

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.frameout.designsystem.theme.FrameAccent

private val INK = Color(0xFF05060F)

internal fun DrawScope.drawBrandTile(t: Float, c: Offset, tile: Float, accent: FrameAccent, paths: SplashPaths) {
    val tileP = outCubic(seg(t, 0f, 320f))
    val glow = (0.16f + 0.22f * outCubic(seg(t, 300f, 1000f))) * tileP
    drawCircle(
        brush = Brush.radialGradient(listOf(accent.a.copy(alpha = glow), Color.Transparent), center = c, radius = tile * 1.25f),
        radius = tile * 1.25f,
        center = c
    )
    val left = c.x - tile / 2f
    val top = c.y - tile / 2f
    val tileScale = 0.8f + 0.2f * tileP
    withTransform({ scale(tileScale, tileScale, c) }) {
        drawTileBody(Offset(left, top), tile, accent, tileP)
        val s = tile * 0.64f / ICON_W
        withTransform({
            translate(c.x - ICON_CX * s, c.y - ICON_CY * s)
            scale(s, s, Offset.Zero)
        }) { drawIcon(t, accent, paths) }
    }
}

private fun DrawScope.drawTileBody(topLeft: Offset, tile: Float, accent: FrameAccent, alpha: Float) {
    val radius = CornerRadius(tile * 0.27f)
    val fill = Brush.linearGradient(
        listOf(lerp(INK, accent.b, 0.20f), lerp(INK, accent.a, 0.08f)),
        topLeft,
        Offset(topLeft.x + tile, topLeft.y + tile)
    )
    drawRoundRect(fill, topLeft, Size(tile, tile), radius, alpha = alpha)
    drawRoundRect(Color.White.copy(alpha = 0.10f * alpha), topLeft, Size(tile, tile), radius, style = Stroke(width = tile * 0.008f))
}
