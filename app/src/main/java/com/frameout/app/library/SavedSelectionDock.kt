package com.frameout.app.library

import androidx.compose.runtime.Composable
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SavedStrings
import com.frameout.designsystem.dock.DockItem
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra

internal class SelectionDockInput(
    val inProjects: Boolean,
    val exporting: Boolean,
    val onProjectsExport: () -> Unit,
    val onShare: () -> Unit,
    val onSaveToGallery: () -> Unit,
    val onSavedExport: () -> Unit,
    val onBusy: () -> Unit,
    val onDelete: () -> Unit
)

@Composable
internal fun buildSelectionDock(input: SelectionDockInput): LibrarySelectionDock = LibrarySelectionDock(
    actionId = if (input.inProjects) "export" else "share",
    actionIcon = if (input.inProjects) FrameIconsExtra.ExportOut else FrameIcons.Share,
    actionLabel = if (input.inProjects) FrameStrings.exportProject() else FrameStrings.share(),
    onAction = {
        if (input.inProjects) { if (input.exporting) input.onBusy() else input.onProjectsExport() }
        else input.onShare()
    },
    onDelete = { if (input.exporting) input.onBusy() else input.onDelete() },
    extra = if (input.inProjects) emptyList() else listOf(
        DockItem("save", SavedStrings.saveToGallery(), FrameIcons.Save, onClick = { if (input.exporting) input.onBusy() else input.onSaveToGallery() }),
        DockItem("export", FrameStrings.groupExport(), FrameIconsExtra.ExportOut, onClick = { if (input.exporting) input.onBusy() else input.onSavedExport() })
    )
)
