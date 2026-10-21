package com.frameout.app.frames

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.quality.FaceAssessment
import com.frameout.core.quality.FaceExplain
import com.frameout.core.quality.FaceFactor
import com.frameout.core.quality.FaceLevel
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.DialogButton
import com.frameout.designsystem.components.DialogTone
import com.frameout.designsystem.components.FrameDialog
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.components.dialogToneColor
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private class Metric(val label: String, val value: Float, val known: Boolean = true)

@Composable
internal fun FaceDetailsDialog(face: FaceAssessment, onClose: () -> Unit) {
    val tone = when (face.level) {
        FaceLevel.Good -> DialogTone.Success
        FaceLevel.Poor -> DialogTone.Danger
        else -> DialogTone.Warning
    }
    val accent = dialogToneColor(tone)
    FrameDialog(
        onDismiss = onClose,
        tone = tone,
        title = when (face.level) {
            FaceLevel.Good -> QualityStrings.faceGood()
            FaceLevel.Poor -> QualityStrings.facePoor()
            else -> QualityStrings.faceFair()
        },
        body = reasonOf(face),
        primary = DialogButton(FrameStrings.close(), onClose),
        badge = { FaceGlyph(face, accent) }
    ) {
        MetricList(face, Modifier.padding(top = 18.dp))
        FrameText(
            QualityStrings.whyFaces(face.faces),
            style = FrameTheme.typography.bodySmall,
            color = FrameTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

private fun reasonOf(a: FaceAssessment): String = when (FaceExplain.factor(a)) {
    FaceFactor.AllGood -> QualityStrings.factorGood()
    FaceFactor.EyesClosed -> QualityStrings.factorEyesClosed()
    FaceFactor.Blurry -> QualityStrings.factorBlurry()
    FaceFactor.TurnedAway -> QualityStrings.factorTurned()
    FaceFactor.SmallFace -> QualityStrings.factorSmall()
    FaceFactor.HalfOpenEyes -> QualityStrings.factorHalfEyes()
    FaceFactor.EyesUnverified -> QualityStrings.factorUnverified()
}

private fun metricsOf(a: FaceAssessment): List<Metric> = buildList {
    add(Metric(QualityStrings.metricEyes(), a.eyesOpen, a.eyesKnown))
    add(Metric(QualityStrings.metricSharp(), a.sharp))
    add(Metric(QualityStrings.metricFrontal(), a.frontal))
    add(Metric(QualityStrings.metricSize(), a.size))
    if (a.smile > 0f) add(Metric(QualityStrings.metricSmile(), a.smile))
}

@Composable
private fun MetricList(face: FaceAssessment, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metricsOf(face).forEachIndexed { index, metric -> MetricRow(metric, index) }
    }
}

@Composable
private fun ColumnScope.MetricRow(metric: Metric, index: Int) {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val fill = remember { Animatable(if (reduce) metric.value else 0f) }
    LaunchedEffect(metric.value) { if (!reduce) fill.animateTo(metric.value.coerceIn(0f, 1f), tween(520, delayMillis = 90 * index)) }
    val tint = if (!metric.known) colors.onSurfaceVariant else levelColor(metric.value)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FrameText(metric.label, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.width(112.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(FrameShapes.pill).background(colors.onSurface.copy(alpha = 0.09f))) {
            if (metric.known) Box(Modifier.fillMaxWidth(fill.value.coerceIn(0f, 1f)).height(8.dp).clip(FrameShapes.pill).background(tint))
        }
        FrameText(
            if (metric.known) "${(metric.value * 100f).toInt().coerceIn(0, 100)}%" else QualityStrings.metricUnknown(),
            style = FrameTheme.typography.label,
            color = tint,
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
private fun levelColor(value: Float): Color {
    val colors = FrameTheme.colors
    return when {
        value >= 0.66f -> colors.success
        value >= 0.40f -> colors.warning
        else -> colors.error
    }
}
