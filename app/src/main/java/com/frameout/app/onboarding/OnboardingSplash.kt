package com.frameout.app.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal fun hash(i: Int): Float = ((i * 7919 + 104729) % 997) / 997f

internal fun DrawScope.drawSplash(phase: Float) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val baseY = h * 0.70f
    val pi = PI.toFloat()
    val lift = sin(pi * phase.coerceIn(0f, 1f))

    drawRect(Brush.verticalGradient(listOf(OnbColors.Ink, Color(0xFF171A45), Color(0xFF0F1030)), startY = 0f, endY = h))
    val bloom = Offset(cx, baseY - h * 0.16f)
    drawCircle(
        Brush.radialGradient(listOf(OnbColors.CyanSoft.copy(alpha = 0.10f + 0.28f * lift), Color.Transparent), bloom, w * 0.55f),
        radius = w * 0.55f,
        center = bloom
    )
    drawRect(
        Brush.verticalGradient(listOf(OnbColors.Indigo.copy(alpha = 0.32f), Color.Transparent), startY = baseY, endY = h),
        topLeft = Offset(0f, baseY),
        size = Size(w, h - baseY)
    )

    for (k in 0 until 3) {
        val f = (phase * 1.5f + k / 3f) % 1f
        val rx = w * (0.08f + 0.44f * f)
        val ry = rx * 0.26f
        drawOval(
            brush = Brush.horizontalGradient(listOf(OnbColors.Violet, OnbColors.CyanSoft, OnbColors.Violet), startX = cx - rx, endX = cx + rx),
            topLeft = Offset(cx - rx, baseY - ry),
            size = Size(rx * 2f, ry * 2f),
            alpha = (1f - f) * 0.85f,
            style = Stroke(width = max(1f, w * 0.006f))
        )
    }

    val n = 11
    val crownH = h * (0.08f + 0.36f * lift)
    val spread = w * (0.10f + 0.20f * lift)
    for (i in 0 until n) {
        val u = i / (n - 1f) * 2f - 1f
        val boost = cos(u * pi / 2f)
        val rise = crownH * (0.45f + 0.55f * boost)
        val baseX = cx + u * w * 0.07f
        val tipX = cx + u * spread
        val tipY = baseY - rise
        val half = w * (0.016f - 0.006f * abs(u))
        val midY = (baseY + tipY) / 2f
        val bend = (tipX - baseX) * 0.1f
        val petal = Path().apply {
            moveTo(baseX - half, baseY)
            quadraticTo(baseX + bend - half * 0.6f, midY, tipX, tipY)
            quadraticTo(baseX + bend + half * 0.6f, midY, baseX + half, baseY)
            close()
        }
        drawPath(
            petal,
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.95f), OnbColors.CyanSoft.copy(alpha = 0.85f), OnbColors.Violet.copy(alpha = 0.55f)),
                startY = tipY,
                endY = baseY
            )
        )
        drawCircle(
            Color.White.copy(alpha = 0.9f),
            radius = w * 0.011f * (1f - 0.4f * abs(u)),
            center = Offset(tipX, tipY - w * 0.02f * lift)
        )
    }

    val colW = w * 0.05f
    val colTop = baseY - crownH * 1.3f
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.95f), OnbColors.CyanSoft, OnbColors.Indigo.copy(alpha = 0.7f)),
            startY = colTop,
            endY = baseY
        ),
        topLeft = Offset(cx - colW / 2f, colTop),
        size = Size(colW, baseY - colTop),
        cornerRadius = CornerRadius(colW / 2f)
    )
    drawCircle(Color.White, radius = colW * 0.62f, center = Offset(cx, colTop - colW * 0.2f))

    for (i in 0 until 16) {
        val a = hash(i * 3 + 1)
        val b = hash(i * 3 + 2)
        val c = hash(i * 3 + 3)
        val twinkle = 0.5f + 0.5f * sin((phase * 6f + c * 6f) * pi)
        drawCircle(
            Color.White.copy(alpha = (0.10f + 0.5f * twinkle * lift).coerceIn(0f, 1f)),
            radius = w * (0.003f + 0.004f * c),
            center = Offset(w * (0.12f + 0.76f * a), h * (0.10f + 0.55f * b))
        )
    }

    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), Offset(cx, h * 0.5f), w * 0.78f))
}
