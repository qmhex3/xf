package com.frameout.app.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private const val ShimmerMs = 2600
private val Gold = Color(0xFFFFD98A)
private val GoldDeep = Color(0xFFE9A93B)

@Composable
internal fun ProBadge(modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val shimmer by rememberInfiniteTransition(label = "pro").animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(ShimmerMs, easing = LinearEasing), RepeatMode.Restart),
        label = "proShimmer"
    )
    val rim = Brush.linearGradient(listOf(Gold.copy(alpha = 0.95f), GoldDeep.copy(alpha = 0.35f), colors.accentC.copy(alpha = 0.6f)))
    val fill = Brush.linearGradient(listOf(GoldDeep.copy(alpha = 0.22f), colors.accentA.copy(alpha = 0.22f)))
    Row(
        modifier
            .clip(FrameShapes.pill)
            .background(fill)
            .border(1.dp, rim, FrameShapes.pill)
            .proShimmer(shimmer, !reduce)
            .padding(start = 7.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spark(Modifier.size(9.dp))
        BasicText(
            "PRO",
            style = FrameTheme.typography.label.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                brush = Brush.verticalGradient(listOf(Color(0xFFFFF1CC), Gold, GoldDeep))
            )
        )
    }
}

private fun Modifier.proShimmer(progress: Float, enabled: Boolean): Modifier = drawWithContent {
    drawContent()
    if (!enabled) return@drawWithContent
    val x = size.width * progress
    val sweep = Brush.linearGradient(
        listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
        start = Offset(x - size.height, 0f),
        end = Offset(x + size.height, size.height)
    )
    drawRect(sweep)
}

@Composable
private fun Spark(modifier: Modifier) {
    Canvas(modifier) {
        val c = size.minDimension / 2f
        val k = c * 0.22f
        val path = Path().apply {
            moveTo(c, 0f)
            quadraticTo(c + k, c - k, size.width, c)
            quadraticTo(c + k, c + k, c, size.height)
            quadraticTo(c - k, c + k, 0f, c)
            quadraticTo(c - k, c - k, c, 0f)
            close()
        }
        drawPath(path, Brush.linearGradient(listOf(Color(0xFFFFF1CC), GoldDeep), Offset.Zero, Offset(size.width, size.height)))
    }
}
