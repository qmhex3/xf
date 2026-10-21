package com.frameout.app.splash

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.frameout.designsystem.theme.FrameAccent
import kotlin.math.PI
import kotlin.math.sin

private val PLATE = Color(0x80010720)

private fun DrawScope.layerIn(p: Float, shiftX: Float, shiftY: Float, block: DrawScope.(Float) -> Unit) {
    val a = seg(p, 0f, 0.45f)
    if (a <= 0f) return
    val k = outBack(p)
    withTransform({
        translate(shiftX * (1f - k), shiftY * (1f - k))
        scale(0.9f + 0.1f * k, 0.9f + 0.1f * k, FRAME_PIVOT)
    }) { block(a) }
}

private fun diag(accent: FrameAccent, sx: Float, sy: Float, ex: Float, ey: Float, mid: Float = 0.5f) =
    Brush.linearGradient(
        colorStops = arrayOf(0f to accent.b, mid to accent.a, 1f to accent.c),
        start = Offset(sx, sy),
        end = Offset(ex, ey)
    )

internal fun DrawScope.drawIcon(t: Float, accent: FrameAccent, paths: SplashPaths) {
    layerIn(seg(t, 100f, 520f), -70f, 70f) { a ->
        drawPath(paths.back, diag(accent, 310f, 778f, 825f, 376f), alpha = a)
    }
    layerIn(seg(t, 220f, 640f), -45f, 45f) { a ->
        drawPath(paths.plateMid, PLATE, alpha = a)
        drawPath(paths.mid, diag(accent, 228f, 706f, 750f, 270f), alpha = a)
    }
    val frameP = seg(t, 340f, 780f)
    val frameA = seg(frameP, 0f, 0.45f)
    if (frameA <= 0f) return
    val k = 0.78f + 0.22f * outBack(frameP)
    withTransform({ scale(k, k, FRAME_PIVOT) }) {
        drawPath(paths.plateFront, PLATE, alpha = frameA)
        drawPath(paths.ring, diag(accent, 166f, 640f, 690f, 205f, 0.42f), alpha = frameA)
        val screenA = outCubic(seg(t, 520f, 820f)) * frameA
        if (screenA > 0f) drawScreen(t, accent, paths, screenA)
    }
}

private fun DrawScope.drawScreen(t: Float, accent: FrameAccent, paths: SplashPaths, alpha: Float) {
    val deep1 = lerp(Color(0xFF040713), accent.b, 0.22f)
    val deep2 = lerp(Color(0xFF060B26), accent.a, 0.30f)
    drawPath(paths.screen, Brush.linearGradient(listOf(deep1, deep2), Offset(220f, 600f), Offset(630f, 260f)), alpha = alpha)
    drawPath(
        paths.screen,
        Brush.radialGradient(listOf(accent.c.copy(alpha = 0.55f), Color.Transparent), center = Offset(590f, 285f), radius = 170f),
        alpha = alpha
    )
    drawMountain(t, accent, paths)
    drawSun(t, accent)
    drawGlint(t, paths)
}

private fun DrawScope.drawMountain(t: Float, accent: FrameAccent, paths: SplashPaths) {
    val p = outCubic(seg(t, 680f, 1020f))
    if (p <= 0f) return
    val fill = Brush.linearGradient(
        listOf(lerp(accent.a, Color.White, 0.15f), lerp(accent.b, Color.White, 0.5f)),
        Offset(262f, 550f),
        Offset(440f, 400f)
    )
    clipPath(paths.screen) { translate(0f, (1f - p) * 90f) { drawPath(paths.mountain, fill, alpha = p) } }
}

private fun DrawScope.drawSun(t: Float, accent: FrameAccent) {
    val sunP = seg(t, 800f, 1120f)
    if (sunP > 0f) {
        val k = outBack(sunP)
        val fill = Brush.linearGradient(
            listOf(lerp(accent.a, Color.White, 0.25f), lerp(accent.c, Color.White, 0.45f)),
            Offset(472f, 392f),
            Offset(534f, 332f)
        )
        withTransform({ scale(k, k, SUN_CENTER) }) { drawCircle(fill, SUN_R, SUN_CENTER) }
    }
    val pulse = outCubic(seg(t, 900f, 1400f))
    if (pulse > 0f && pulse < 1f) {
        drawCircle(
            color = accent.c.copy(alpha = 0.8f * (1f - pulse)),
            radius = SUN_R + 95f * pulse,
            center = SUN_CENTER,
            style = Stroke(width = 5f * (1f - pulse) + 0.5f)
        )
    }
}

private fun DrawScope.drawGlint(t: Float, paths: SplashPaths) {
    val glint = inOut(seg(t, 980f, 1450f))
    if (glint <= 0f || glint >= 1f) return
    val mx = 150f + 650f * glint
    val my = 640f - 450f * glint
    val band = Brush.linearGradient(
        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.55f * sin(PI.toFloat() * glint)), Color.Transparent),
        start = Offset(mx - 90f, my + 90f),
        end = Offset(mx + 90f, my - 90f)
    )
    clipPath(paths.ring) { drawRect(band, Offset(100f, 150f), Size(800f, 700f)) }
    clipPath(paths.screen) { drawRect(band, Offset(100f, 150f), Size(800f, 700f)) }
}
