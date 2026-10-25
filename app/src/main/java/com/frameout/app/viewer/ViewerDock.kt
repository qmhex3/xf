package com.frameout.app.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frameout.core.util.EditStrings
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SearchStrings
import com.frameout.designsystem.dock.DockItem
import com.frameout.designsystem.dock.FrameDock
import com.frameout.designsystem.dock.FrameFab
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra

@Composable
fun ViewerDock(
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onSearch: () -> Unit = {},
    onEdit: () -> Unit = {},
    playing: Boolean = false,
    onPlay: () -> Unit = {},
    showPlay: Boolean = true,
    showGoTo: Boolean = false,
    onGoTo: () -> Unit = {}
) {
    val items = listOfNotNull(
        if (!showPlay) null else DockItem("play", if (playing) FrameStrings.pause() else FrameStrings.play(), FrameIcons.PlayFilled, selectedIcon = FrameIcons.PauseFilled, selected = playing, onClick = onPlay),
        if (!showGoTo) null else DockItem("goto", FrameStrings.showInProject(), FrameIconsExtra.GoToFrame, onClick = onGoTo),
        DockItem("save", FrameStrings.save(), FrameIcons.Save, onClick = onSave),
        DockItem("share", FrameStrings.share(), FrameIcons.Share, onClick = onShare),
        DockItem("search", SearchStrings.actionShort(), FrameIcons.Search, onClick = onSearch),
        DockItem("delete", FrameStrings.delete(), FrameIcons.Trash, destructive = true, onClick = onDelete)
    )
    FrameDock(
        items = items,
        modifier = Modifier.fillMaxSize(),
        compactLimit = 8,
        fab = { FrameFab(FrameIconsExtra.Edit, EditStrings.tools(), onClick = onEdit) },
        fabLabel = EditStrings.tools()
    )
}
