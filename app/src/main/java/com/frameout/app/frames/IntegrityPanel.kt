package com.frameout.app.frames

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.quality.FrameIntegrity
import com.frameout.core.quality.FrameIntegrity.Finding
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import java.util.Locale

fun integrityLines(report: FrameIntegrity.Report): List<String> {
    val lines = ArrayList<String>()
    lines += report.expected?.let { QualityStrings.integrityCount(report.actual, it) } ?: QualityStrings.integrityCountOnly(report.actual)
    report.measuredFps?.let { lines += QualityStrings.integrityRate(String.format(Locale.US, "%.2f", it)) }
    if (report.healthy) lines += QualityStrings.integrityOk()
    report.findings.forEach { finding ->
        lines += when (finding) {
            Finding.CountMismatch -> QualityStrings.integrityMismatch(kotlin.math.abs((report.expected ?: report.actual) - report.actual))
            Finding.TailShort -> QualityStrings.integrityTail(String.format(Locale.US, "%.2f", report.tailMs / 1000.0))
            Finding.VariableRate -> QualityStrings.integrityVfr(String.format(Locale.US, "%.1f", report.minGapMs), String.format(Locale.US, "%.1f", report.maxGapMs))
            Finding.DuplicateTimestamps -> QualityStrings.integrityDuplicates(report.duplicateTimestamps)
            Finding.SlowMotion -> QualityStrings.integritySlow(
                String.format(Locale.US, "%.0f", report.captureFps ?: 0.0),
                String.format(Locale.US, "%.0f", report.measuredFps ?: 0.0)
            )
        }
    }
    return lines
}

@Composable
fun IntegrityPanel(report: FrameIntegrity.Report, onClose: () -> Unit) {
    val colors = FrameTheme.colors
    Box(Modifier.fillMaxSize().frameClickable(onClick = onClose), contentAlignment = Alignment.Center) {
        GlassSurface(shape = FrameShapes.lg, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FrameText(QualityStrings.integrityTitle(), style = FrameTheme.typography.label, color = colors.primary)
                integrityLines(report).forEach { FrameText(it, color = colors.onSurface) }
                FrameText(QualityStrings.integrityClose(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}
