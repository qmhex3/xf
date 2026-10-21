package com.frameout.app.picker

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun PickerHeader(
    state: PickerState,
    vm: PickerViewModel,
    onHeight: (Int) -> Unit,
    onClose: () -> Unit,
    onSaf: () -> Unit,
    onManageAccess: (() -> Unit)?,
    modifier: Modifier
) {
    val bg = FrameTheme.colors.background
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(bg.copy(alpha = 0.94f), bg.copy(alpha = 0.78f), Color.Transparent)))
            .onSizeChanged { onHeight(it.height) }
            .padding(bottom = 16.dp)
    ) {
        PickerTopBar(onClose, onSaf, onManageAccess)
        if (state.permissionGranted) PickerFilters(state, vm)
    }
}

@Composable
private fun PickerFilters(state: PickerState, vm: PickerViewModel) {
    Column(Modifier.animateContentSize(frameSpring())) {
        PickerFilterBar(state.albums, state.selectedBucket, vm::selectAlbum, state.filters, state.selectedKind, vm::setFilter)
    }
}
