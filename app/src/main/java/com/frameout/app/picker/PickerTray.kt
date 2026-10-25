package com.frameout.app.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frameout.core.media.MediaItem

@Composable
internal fun PickerTray(
    selected: List<MediaItem>,
    vm: PickerViewModel,
    onContinue: () -> Unit,
    modifier: Modifier
) {
    SelectionTray(
        selected,
        onRemove = vm::toggle,
        onContinue = onContinue,
        modifier = modifier,
        onMove = vm::moveSelected
    )
}
