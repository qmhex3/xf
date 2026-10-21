package com.frameout.app.library

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import com.frameout.core.db.SavedKind
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra

@DrawableRes
internal fun SavedKind.icon(): Int = when (this) {
    SavedKind.Photo -> FrameIconsExtra.ImageFormat
    SavedKind.Gif -> FrameIconsExtra.Gif
    SavedKind.Motion -> FrameIconsExtra.Motion
}

@Composable
internal fun SavedKind.label(): String = when (this) {
    SavedKind.Photo -> FrameStrings.mediaPhoto()
    SavedKind.Gif -> "GIF"
    SavedKind.Motion -> FrameStrings.mediaMotion()
}

@Composable
internal fun SavedKindOptions(state: LibraryState, onKind: (SavedKind?) -> Unit) {
    val present = SavedKind.values().filter { (state.savedKindCounts[it] ?: 0) > 0 }
    if (present.size < 2) return
    MenuTitle(FrameStrings.filterByType())
    MenuOption(FrameStrings.allMedia(), state.savedKind == null, FrameIcons.GridSize) { onKind(null) }
    present.forEach { kind ->
        MenuOption("${kind.label()}  ${state.savedKindCounts[kind]}", state.savedKind == kind, kind.icon()) { onKind(kind) }
    }
}
