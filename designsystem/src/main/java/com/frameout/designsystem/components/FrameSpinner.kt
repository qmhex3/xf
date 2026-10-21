package com.frameout.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.theme.FrameTheme

private const val SPINNER_SWEEP = 100f
private const val SPINNER_MS = 1100

@Composable
fun FrameSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = FrameTheme.colors.primary,
    strokeWidth: Dp = 3.dp
) {
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val angle = if (reduceMotion) {
        0f
    } else {
        val value by rememberInfiniteTransition(label = "spinner").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(SPINNER_MS, easing = LinearEasing)),
            label = "spinnerAngle"
        )
        value
    }
    Canvas(modifier.size(size)) {
        val stroke = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
        drawArc(color.copy(alpha = 0.25f), 0f, 360f, false, style = stroke)
        drawArc(color, angle - 90f, if (reduceMotion) 270f else SPINNER_SWEEP, false, style = stroke)
    }
}
