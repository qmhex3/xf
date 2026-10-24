package com.frameout.app.viewer

import com.frameout.core.db.FrameEditEntity
import com.frameout.core.edit.Transform
import com.frameout.core.edit.toEdit
import kotlinx.coroutines.flow.update

internal const val MAX_PREFETCH_BYTES = 64 * 1024 * 1024
internal const val FULL_AHEAD = 5
internal const val WARM_AHEAD = 8
internal const val WARM_BEHIND = 3
internal fun fromEntity(e: FrameEditEntity) = e.toEdit()

fun ViewerViewModel.cyclePlaySpeed() = _state.update { it.copy(playFps = nextPlayFps(it.playFps)) }
fun ViewerViewModel.togglePixelGrid() = _state.update { it.copy(showPixelGrid = !it.showPixelGrid) }
fun ViewerViewModel.toggleDetections() = _state.update { it.copy(showDetections = !it.showDetections) }
fun ViewerViewModel.toggleControls() = _state.update { it.copy(controlsVisible = !it.controlsVisible) }
fun ViewerViewModel.setTransform(scale: Float, ox: Float, oy: Float) = _state.update { it.copy(scale = scale, offsetX = ox, offsetY = oy) }
fun ViewerViewModel.setTransform(t: Transform) = setTransform(t.scale, t.x, t.y)
fun ViewerViewModel.transformNow() = _state.value.transform()
fun ViewerViewModel.dismissUndo() {
    savedDelete.commit()
    _state.update { it.copy(deletedStack = emptyList(), savedUndoCount = 0) }
}
fun ViewerViewModel.clearMessage() = _state.update { if (it.messageBusy) it else it.copy(message = null, busyMessage = null, messageSaved = null, messageIcon = null) }
