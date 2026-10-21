package com.frameout.app.billing

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedCheck(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (progress <= 0f) return@Canvas
        val check = Path().apply {
            moveTo(size.width * 0.17f, size.height * 0.54f)
            lineTo(size.width * 0.38f, size.height * 0.76f)
            lineTo(size.width * 0.83f, size.height * 0.28f)
        }
        val measure = PathMeasure().apply { setPath(check, false) }
        val part = Path()
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), part, true)
        drawPath(part, color, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
