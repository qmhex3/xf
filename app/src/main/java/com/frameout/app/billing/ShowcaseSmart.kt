package com.frameout.app.billing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.frameout.core.pro.ProShowcase
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme

private enum class FrameVerdict { Blurry, Duplicate, Sharp }

private class SmartFrame(val hueTop: Float, val hueBottom: Float, val verdict: FrameVerdict)

private val Strip = listOf(
    SmartFrame(210f, 260f, FrameVerdict.Sharp),
    SmartFrame(30f, 60f, FrameVerdict.Blurry),
    SmartFrame(30f, 60f, FrameVerdict.Duplicate),
    SmartFrame(170f, 220f, FrameVerdict.Sharp),
    SmartFrame(310f, 340f, FrameVerdict.Blurry),
    SmartFrame(310f, 340f, FrameVerdict.Duplicate),
    SmartFrame(50f, 100f, FrameVerdict.Sharp),
    SmartFrame(110f, 170f, FrameVerdict.Sharp)
)
private val Gray = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
private const val CYCLE_MS = 6000
private const val JUDGE_FROM = 0.3f
private const val JUDGE_TO = 0.88f

@Composable
internal fun SmartShowcase() {
    val reduce = LocalSystemA11y.current.reduceMotion
    val phase = rememberJudgePhase(reduce)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Strip.forEach { FrameCell(it, phase.value) }
        }
        FrameText(ProShowcase.smartCount(), FrameTheme.typography.titleSmall, FrameTheme.colors.onSurface, Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun rememberJudgePhase(reduce: Boolean): State<Float> {
    if (reduce) return remember { mutableFloatStateOf(0.6f) }
    val transition = rememberInfiniteTransition(label = "judge")
    return transition.animateFloat(0f, 1f, infiniteRepeatable(tween(CYCLE_MS, easing = LinearEasing)), label = "judgeValue")
}

private fun judged(phase: Float): Float = ((phase - JUDGE_FROM) / 0.12f).coerceIn(0f, 1f) * (1f - ((phase - JUDGE_TO) / 0.12f).coerceIn(0f, 1f))

@Composable
private fun FrameCell(frame: SmartFrame, phase: Float) {
    val amount = judged(phase)
    val filter = if (frame.verdict == FrameVerdict.Duplicate && amount > 0.5f) Gray else null
    val dim = if (frame.verdict == FrameVerdict.Sharp) 0f else amount
    val lift = if (frame.verdict == FrameVerdict.Sharp) -7f * amount else 0f
    Box(Modifier.size(width = 30.dp, height = 46.dp).graphicsLayer { translationY = lift.dp.toPx(); alpha = 1f - 0.55f * dim }, Alignment.Center) {
        Canvas(Modifier.size(width = 30.dp, height = 46.dp)) {
            val top = Color.hsv(frame.hueTop, 0.55f, 0.85f)
            val bottom = Color.hsv(frame.hueBottom, 0.6f, 0.5f)
            drawRoundRect(Brush.verticalGradient(listOf(top, bottom)), cornerRadius = CornerRadius(7.dp.toPx()), colorFilter = filter)
            val ring = if (frame.verdict == FrameVerdict.Sharp) Color(0xFF4BE3A3).copy(alpha = amount) else Color.White.copy(alpha = 0.25f)
            drawRoundRect(ring, cornerRadius = CornerRadius(7.dp.toPx()), style = Stroke(if (frame.verdict == FrameVerdict.Sharp) 2.dp.toPx() else 1.dp.toPx()))
            if (frame.verdict == FrameVerdict.Blurry) drawLine(Color.White.copy(alpha = 0.55f * amount), Offset(4.dp.toPx(), size.height * 0.5f), Offset(size.width - 4.dp.toPx(), size.height * 0.5f), 6.dp.toPx())
        }
    }
}
