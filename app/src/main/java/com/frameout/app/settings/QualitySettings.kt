package com.frameout.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.storage.ImageFormat
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameSlider
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.roundToInt

private const val MIN_QUALITY = 30
private const val MAX_QUALITY = 100
private const val SMALL = 75
private const val BALANCED = 90

@Composable
internal fun QualitySettings(prefs: AppPrefs, formatKey: String) {
    val colors = FrameTheme.colors
    val quality by prefs.jpegQuality.collectAsState()
    val lossless = ImageFormat.fromKey(formatKey).lossless
    val shown = if (lossless) MAX_QUALITY else quality.coerceIn(MIN_QUALITY, MAX_QUALITY)
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { SettingsRowHeader(FrameIconsExtra.Contrast, FrameStrings.qualityTitle()) }
            FrameText("$shown%", style = FrameTheme.typography.mono, color = colors.primary)
        }
        if (lossless) {
            FrameText(FrameStrings.qualityLosslessHint(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        } else {
            SettingsSegmented(
                listOf(
                    SegmentOption(SMALL.toString(), FrameStrings.qualitySmall()),
                    SegmentOption(BALANCED.toString(), FrameStrings.qualityBalanced()),
                    SegmentOption(MAX_QUALITY.toString(), FrameStrings.qualityMax())
                ),
                shown.toString()
            ) { prefs.setJpegQuality(it.toInt()) }
            FrameSlider(
                value = shown.toFloat(),
                onChange = { prefs.setJpegQuality(it.roundToInt().coerceIn(MIN_QUALITY, MAX_QUALITY)) },
                min = MIN_QUALITY.toFloat(),
                max = MAX_QUALITY.toFloat(),
                label = FrameStrings.qualityTitle(),
                modifier = Modifier.height(40.dp)
            )
            FrameText(FrameStrings.qualityHint(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}
