package com.frameout.app.frames

import com.frameout.core.edit.FrameEdit
import com.frameout.core.util.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class FramesEdits(
    private val scope: CoroutineScope,
    private val container: AppContainer,
    private val state: StateFlow<FramesState>
) {
    fun transform(transform: (FrameEdit) -> FrameEdit) {
        val ids = state.value.selected.toList()
        if (ids.isEmpty()) return
        scope.launch { applyEditsToIds(container.editHistory, ids, transform) }
    }

    fun history(op: suspend (List<Long>) -> Unit) {
        val ids = state.value.selected.toList()
        if (ids.isEmpty()) return
        scope.launch { op(ids) }
    }
}
