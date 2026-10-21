package com.frameout.app.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.media.MediaItem
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.PickerStrings
import com.frameout.designsystem.components.FrameToastHost
import com.frameout.designsystem.components.ToastAction
import com.frameout.designsystem.components.ToastKind
import com.frameout.designsystem.icons.FrameIcons
import kotlinx.coroutines.delay

internal class PickerUndo(val message: String, val item: MediaItem, val index: Int)

@Stable
internal class PickerUndoState {
    var current by mutableStateOf<PickerUndo?>(null)
    var last by mutableStateOf<PickerUndo?>(null)

    fun push(message: String, item: MediaItem, index: Int) {
        val undo = PickerUndo(message, item, index)
        current = undo
        last = undo
    }

    fun clear() {
        current = null
    }
}

private const val UNDO_MS = 6000L

@Composable
internal fun rememberPickerUndo(): PickerUndoState {
    val state = remember { PickerUndoState() }
    LaunchedEffect(state.current) {
        if (state.current != null) {
            delay(UNDO_MS)
            state.clear()
        }
    }
    return state
}

@Composable
internal fun PickerTray(
    selected: List<MediaItem>,
    vm: PickerViewModel,
    undo: PickerUndoState,
    onContinue: () -> Unit,
    modifier: Modifier
) {
    SelectionTray(
        selected,
        onRemove = { item ->
            undo.push(PickerStrings.itemRemoved(item.displayName), item, selected.indexOfFirst { it.uri == item.uri })
            vm.toggle(item)
        },
        onContinue = onContinue,
        modifier = modifier,
        onMove = vm::moveSelected
    )
}

@Composable
internal fun PickerUndoToast(
    undo: PickerUndoState,
    onRestore: (MediaItem, Int) -> Unit,
    modifier: Modifier
) {
    FrameToastHost(
        undo.current?.message,
        modifier,
        kind = ToastKind.Success,
        icon = FrameIcons.Trash,
        durationMs = UNDO_MS,
        actions = listOf(
            ToastAction(FrameStrings.undo(), {
                undo.last?.let { onRestore(it.item, it.index) }
                undo.clear()
            })
        )
    )
}
