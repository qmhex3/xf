package com.frameout.app.library

import androidx.compose.runtime.Composable
import com.frameout.app.frames.FramesSheet
import com.frameout.app.settings.NavRow
import com.frameout.app.settings.SettingsCard
import com.frameout.app.settings.SettingsDivider
import com.frameout.app.settings.SettingsSection
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.icons.FrameIconsExtra

internal enum class ProjectExportKind { Backup, Zip, Video }

internal class ProjectExportChoices(val onBackup: () -> Unit, val onZip: () -> Unit, val onVideo: () -> Unit)

@Composable
internal fun ProjectExportSheet(visible: Boolean, count: Int, choices: ProjectExportChoices, onClose: () -> Unit) {
    FramesSheet(visible, FrameStrings.exportSheetTitle() + FrameStrings.sep() + FrameStrings.selectedCount(count), onClose) {
        SettingsSection(FrameStrings.groupExport()) {
            SettingsCard {
                NavRow(FrameIconsExtra.Folder, FrameStrings.exportBackup(), hint = FrameStrings.exportBackupHint(), onClick = { onClose(); choices.onBackup() })
                SettingsDivider()
                NavRow(FrameIconsExtra.Storage, FrameStrings.exportZip(), hint = FrameStrings.exportZipHint(), onClick = { onClose(); choices.onZip() })
                SettingsDivider()
                NavRow(FrameIconsExtra.Live, FrameStrings.exportVideo(), hint = FrameStrings.exportVideoHint(), onClick = { onClose(); choices.onVideo() })
            }
        }
    }
}
