package com.frameout.app.frames

import android.app.Application
import android.net.Uri
import com.frameout.app.viewer.FrameBitmapLoader
import com.frameout.core.db.FrameEntity
import com.frameout.core.edit.FrameEdit
import com.frameout.core.util.AppContainer
import com.frameout.core.util.FrameStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class FramesCropAll(
    private val scope: CoroutineScope,
    private val container: AppContainer,
    private val state: MutableStateFlow<FramesState>,
    private val app: Application,
    private val selectedFrames: suspend () -> List<FrameEntity>
) {
    fun open() {
        if (state.value.cropAll != null) return
        scope.launch {
            val frames = selectedFrames().sortedBy { it.index }
            val first = frames.firstOrNull() ?: return@launch
            val sourceUri = container.projects.getById(first.projectId)?.sourceUri?.let(Uri::parse)
            val bitmap = withContext(Dispatchers.IO) { FrameBitmapLoader(app).load(first, sourceUri, FrameEdit(first.id)) } ?: return@launch
            state.update { it.copy(cropAll = CropAllState(bitmap, frames.size)) }
        }
    }

    fun update(l: Float, t: Float, r: Float, b: Float) = state.update { s -> s.copy(cropAll = s.cropAll?.withRect(l, t, r, b)) }

    fun reset() = state.update { s -> s.copy(cropAll = s.cropAll?.withRect(0f, 0f, 1f, 1f)) }

    fun cancel() {
        val crop = state.value.cropAll ?: return
        state.update { it.copy(cropAll = null) }
        crop.bitmap.takeUnless { it.isRecycled }?.recycle()
    }

    fun apply() {
        val crop = state.value.cropAll ?: return
        val ids = state.value.selected.toList()
        state.update { it.copy(cropAll = null) }
        scope.launch {
            applyEditsToIds(container.editHistory, ids) { it.copy(cropL = crop.l, cropT = crop.t, cropR = crop.r, cropB = crop.b) }
            crop.bitmap.takeUnless { it.isRecycled }?.recycle()
            state.update { it.copy(message = FrameStrings.cropAllApplied(ids.size), messageIcon = com.frameout.designsystem.icons.FrameIcons.Crop) }
        }
    }
}
