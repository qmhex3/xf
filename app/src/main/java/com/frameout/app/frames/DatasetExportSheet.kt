package com.frameout.app.frames

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.app.collage.ChoiceRow
import com.frameout.app.settings.SettingsActionButton
import com.frameout.app.settings.SettingsCard
import com.frameout.app.settings.SettingsDivider
import com.frameout.app.settings.SettingsSection
import com.frameout.app.settings.ToggleRow
import com.frameout.core.export.DatasetFormat
import com.frameout.core.quality.BlurLevel
import com.frameout.core.quality.DatasetOptions
import com.frameout.core.util.DatasetStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme

private val INTERVALS_MS = listOf(500L, 1000L, 2000L, 5000L)
private val FORMATS = listOf(DatasetFormat.Colmap, DatasetFormat.Yolo, DatasetFormat.Coco)
private val BLURS = listOf(BlurLevel.Off, BlurLevel.Normal, BlurLevel.Strict)

private fun intervalLabel(ms: Long): String = DatasetStrings.seconds(if (ms % 1000L == 0L) (ms / 1000L).toString() else "0.5")

private fun formatLabel(format: DatasetFormat): String = when (format) {
    DatasetFormat.Colmap -> DatasetStrings.colmap()
    DatasetFormat.Yolo -> DatasetStrings.yolo()
    DatasetFormat.Coco -> DatasetStrings.coco()
}

private fun formatHint(format: DatasetFormat): String = when (format) {
    DatasetFormat.Colmap -> DatasetStrings.colmapHint()
    DatasetFormat.Yolo -> DatasetStrings.yoloHint()
    DatasetFormat.Coco -> DatasetStrings.cocoHint()
}

private fun blurLabel(level: BlurLevel): String = when (level) {
    BlurLevel.Off -> DatasetStrings.blurOff()
    BlurLevel.Normal -> DatasetStrings.blurNormal()
    BlurLevel.Strict -> DatasetStrings.blurStrict()
}

@Composable
internal fun DatasetExportSheet(visible: Boolean, frameCount: Int, onClose: () -> Unit, onCreate: (DatasetFormat, DatasetOptions) -> Unit) {
    var format by remember { mutableStateOf(DatasetFormat.Colmap) }
    var interval by remember { mutableStateOf(1000L) }
    var blur by remember { mutableStateOf(BlurLevel.Normal) }
    var dedupe by remember { mutableStateOf(true) }
    var keepRejected by remember { mutableStateOf(false) }
    FramesSheet(visible, DatasetStrings.title(), onClose) {
        FrameText(
            DatasetStrings.hint() + if (frameCount > 0) "  ·  " + com.frameout.core.util.FrameStrings.frameCount(frameCount) else "",
            style = FrameTheme.typography.bodySmall,
            color = FrameTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        SettingsSection(DatasetStrings.target()) {
            ChoiceRow(FORMATS.map { formatLabel(it) }, FORMATS.indexOf(format)) { format = FORMATS[it] }
            FrameText(formatHint(format), style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
        }
        SettingsSection(DatasetStrings.interval()) {
            ChoiceRow(INTERVALS_MS.map { intervalLabel(it) }, INTERVALS_MS.indexOf(interval)) { interval = INTERVALS_MS[it] }
        }
        SettingsSection(DatasetStrings.blur()) {
            ChoiceRow(BLURS.map { blurLabel(it) }, BLURS.indexOf(blur)) { blur = BLURS[it] }
        }
        SettingsCard {
            ToggleRow(FrameIconsExtra.Duplicate, DatasetStrings.duplicates(), DatasetStrings.duplicatesHint(), dedupe) { dedupe = it }
            SettingsDivider()
            ToggleRow(FrameIconsExtra.Blur, DatasetStrings.rejected(), DatasetStrings.rejectedHint(), keepRejected) { keepRejected = it }
        }
        FrameText(DatasetStrings.scope(), style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
        Box(Modifier.padding(top = 8.dp)) {
            SettingsActionButton(DatasetStrings.create()) {
                onClose()
                onCreate(format, DatasetOptions(interval * 1000L, blur, dedupe, keepRejected))
            }
        }
    }
}
