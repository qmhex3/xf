package com.frameout.app.frames

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.frameout.core.quality.FaceAssessment
import com.frameout.core.quality.FaceLevel
import com.frameout.designsystem.a11y.LocalSystemA11y

@Composable
internal fun FaceGlyph(assessment: FaceAssessment, accent: Color, modifier: Modifier = Modifier) {
    val reduce = LocalSystemA11y.current.reduceMotion
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(assessment) { if (!reduce) progress.animateTo(1f, tween(750)) }
    Canvas(modifier.size(112.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val ring = size.minDimension / 2f - 5.dp.toPx()
        drawScoreRing(center, ring, accent, assessment.score.coerceIn(0f, 1f) * progress.value)
        drawFace(center, ring - 13.dp.toPx(), accent, assessment, progress.value)
    }
}

private fun DrawScope.drawScoreRing(center: Offset, radius: Float, accent: Color, fraction: Float) {
    val stroke = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
    val topLeft = Offset(center.x - radius, center.y - radius)
    val box = Size(radius * 2f, radius * 2f)
    drawArc(accent.copy(alpha = 0.16f), 0f, 360f, false, topLeft, box, style = stroke)
    if (fraction > 0.01f) drawArc(accent, -90f, 360f * fraction, false, topLeft, box, style = stroke)
}

private fun DrawScope.drawFace(center: Offset, radius: Float, accent: Color, a: FaceAssessment, reveal: Float) {
    drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.32f), accent.copy(alpha = 0.10f)), center, radius), radius, center)
    drawCircle(accent.copy(alpha = 0.28f), radius, center, style = Stroke(1.dp.toPx()))
    val open = (if (a.eyesKnown) a.eyesOpen else 0.8f).coerceIn(0f, 1f)
    val eyeW = radius * 0.24f
    val eyeH = maxOf(2.5.dp.toPx(), radius * 0.30f * open * reveal)
    val eyeY = center.y - radius * 0.20f
    listOf(-1f, 1f).forEach { side ->
        val x = center.x + side * radius * 0.40f
        drawOval(accent, Offset(x - eyeW / 2f, eyeY - eyeH / 2f), Size(eyeW, eyeH))
    }
    drawMouth(center, radius, accent, mouthCurve(a) * reveal)
}

private fun mouthCurve(a: FaceAssessment): Float = when {
    a.smile > 0.15f -> a.smile.coerceIn(0f, 1f)
    a.level == FaceLevel.Poor -> -0.5f
    a.level == FaceLevel.Good -> 0.35f
    else -> 0f
}

private fun DrawScope.drawMouth(center: Offset, radius: Float, accent: Color, curve: Float) {
    val y = center.y + radius * 0.36f
    val half = radius * 0.40f
    val path = Path().apply {
        moveTo(center.x - half, y)
        quadraticTo(center.x, y + curve * radius * 0.62f, center.x + half, y)
    }
    drawPath(path, accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
}
