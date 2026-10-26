package com.frameout.app.viewer

import android.graphics.Bitmap
import android.net.Uri
import com.frameout.core.edit.BitmapEditRenderer
import com.frameout.core.edit.CropPreset
import com.frameout.core.edit.CropRatio
import com.frameout.core.edit.EditHistoryRepository
import com.frameout.core.edit.FilterPresets
import com.frameout.core.edit.FrameEdit
import com.frameout.core.edit.toEdit
import com.frameout.core.edit.toEntity
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class ViewerToolEditor(
    private val scope: CoroutineScope,
    internal val state: MutableStateFlow<ViewerState>,
    private val loader: FrameBitmapLoader,
    private val history: EditHistoryRepository,
    private val sourceUri: () -> Uri?,
    private val reload: suspend () -> Unit,
    private val handOff: (Long, FrameEdit, Bitmap) -> Unit = { _, _, bitmap -> bitmap.recycle() }
) {
    private var raw: Bitmap? = null
    private var retired: Bitmap? = null
    private var generation = 0L
    private var closeToken = 0L
    internal var viewW = 0f
    internal var viewH = 0f

    fun open(tool: EditTool = EditTool.Adjust) {
        if (state.value.toolBusy) return
        val frame = state.value.current ?: return
        val edit = state.value.edit
        scope.launch {
            val source = withContext(Dispatchers.IO) { loader.load(frame, sourceUri(), FrameEdit(frame.id)) }
            if (source == null) {
                if (state.value.current?.id == frame.id) state.update { it.copy(message = FrameStrings.viewerLoadFailed(), busyMessage = null, messageSaved = null, messageIcon = null) }
                return@launch
            }
            if (state.value.current?.id != frame.id || state.value.tool != EditTool.None || state.value.toolBusy) {
                source.recycle()
                return@launch
            }
            closeToken++
            release()
            raw = source
            val draft = EditDraft(
                edit.brightness, edit.contrast, edit.saturation, edit.filter, edit.filterStrength, edit.marks,
                rotation = edit.rotation, flipH = edit.flipH, flipV = edit.flipV
            )
            state.update {
                it.copy(
                    tool = tool, draft = draft, sourceW = source.width, sourceH = source.height,
                    scale = 1f, offsetX = 0f, offsetY = 0f, controlsVisible = true
                )
            }
            rerender()
        }
    }

    val shapes = ShapeEditor(state) { rerender() }

    fun select(tool: EditTool) {
        if (state.value.toolBusy) return
        val before = state.value.tool
        val liveBefore = state.value.shapesLive()
        val moved = before != tool
        state.update {
            val next = it.copy(tool = tool, draft = dropBlankTexts(it.draft.copy(pickerOpen = false), -1))
            val cleared = next.copy(draft = next.draft.copy(selectedShape = -1))
            if (moved) cleared.copy(scale = 1f, offsetX = 0f, offsetY = 0f) else cleared
        }
        if (liveBefore != state.value.shapesLive()) rerender()
        if ((before == EditTool.Text) != (tool == EditTool.Text) || before.canvasSpace != tool.canvasSpace || before.isColorTool != tool.isColorTool) rerender()
    }

    fun rotate(degrees: Int) {
        if (state.value.toolBusy) return
        update { it.copy(rotation = GeometryMath.normalize(it.rotation + degrees)) }
        rerender()
    }

    fun flipHorizontal() {
        if (state.value.toolBusy) return
        update { d -> GeometryMath.toggleDisplayFlipH(d.rotation, d.flipH, d.flipV).let { (h, v) -> d.copy(flipH = h, flipV = v) } }
        rerender()
    }

    fun flipVertical() {
        if (state.value.toolBusy) return
        update { d -> GeometryMath.toggleDisplayFlipV(d.rotation, d.flipH, d.flipV).let { (h, v) -> d.copy(flipH = h, flipV = v) } }
        rerender()
    }

    fun resetRotate() {
        if (state.value.toolBusy) return
        update { it.copy(rotation = 0, flipH = false, flipV = false) }
        rerender()
    }

    fun setCropRatio(preset: CropPreset) {
        if (state.value.toolBusy) return
        val shown = state.value.bitmap ?: return
        state.update { it.withDisplayCrop(CropRatio.centered(preset, shown.width, shown.height)) }
    }

    fun updateCrop(l: Float, t: Float, r: Float, b: Float) =
        state.update { if (it.toolBusy) it else it.withDisplayCrop(CropRatio.clamp(l, t, r, b)) }

    fun resetCrop() = state.update { if (it.toolBusy) it else it.withoutCrop() }
    fun setPanelHeight(px: Int) = state.update { if (it.panelPx == px) it else it.copy(panelPx = px) }
    fun setViewport(width: Float, height: Float) { viewW = width; viewH = height }

    private fun draftEdit(current: ViewerState, frameId: Long): FrameEdit {
        val draft = current.draft.let { dropBlankTexts(it, -1) }
        return current.edit.copy(
            frameId = frameId,
            brightness = draft.brightness,
            contrast = draft.contrast,
            saturation = draft.saturation,
            filter = draft.filter,
            filterStrength = if (draft.filter == FilterPresets.NONE) FilterPresets.DEFAULT_STRENGTH else draft.filterStrength,
            marks = draft.marks,
            rotation = draft.rotation,
            flipH = draft.flipH,
            flipV = draft.flipV,
            cropL = current.cropL,
            cropT = current.cropT,
            cropR = current.cropR,
            cropB = current.cropB
        )
    }

    private fun hasChanges(): Boolean {
        val current = state.value
        val frame = current.current ?: return false
        return draftEdit(current, frame.id) != current.edit
    }

    fun requestCancel() {
        val current = state.value
        if (current.toolBusy || current.tool == EditTool.None) return
        if (hasChanges()) state.update { it.copy(discardPrompt = true) } else cancel()
    }

    fun keepEditing() = state.update { it.copy(discardPrompt = false) }

    fun commit() {
        val current = state.value
        if (current.toolBusy || current.tool == EditTool.None) return
        val frame = current.current ?: return
        val edit = draftEdit(current, frame.id)
        if (edit == current.edit) return cancel()
        val token = ++closeToken
        state.update { it.copy(toolBusy = true) }
        scope.launch {
            var saved = false
            try {
                val entity = history.save(edit.toEntity())
                saved = true
                val committed = entity.toEdit()
                state.update { it.copy(edit = committed) }
                renderFinal(frame.id, committed)
                release()
                reload()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logFailure("ViewerToolCommit", e)
                if (!saved && token == closeToken) {
                    state.update { it.copy(toolBusy = false, message = FrameStrings.saveFailed(), busyMessage = null) }
                    return@launch
                }
            } finally {
                if (saved) finishClose(token)
            }
        }
    }

    fun cancel() {
        val current = state.value
        if (current.toolBusy || current.tool == EditTool.None) return
        val token = ++closeToken
        state.update { it.copy(toolBusy = true, discardPrompt = false) }
        scope.launch {
            try {
                current.current?.let { renderFinal(it.id, current.edit) }
                release()
                reload()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logFailure("ViewerToolCancel", e)
            } finally {
                finishClose(token)
            }
        }
    }

    private suspend fun renderFinal(frameId: Long, edit: FrameEdit) {
        val source = raw ?: return
        generation++
        val result = try {
            renderLock.withLock {
                withContext(Dispatchers.Default) { if (edit.isIdentity) source else BitmapEditRenderer.render(source, edit) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFailure("ViewerToolFinal", e)
            return
        } catch (e: OutOfMemoryError) {
            logFailure("ViewerToolFinal", e)
            return
        }
        raw = null
        if (result !== source) source.takeUnless { it.isRecycled }?.recycle()
        if (result.isRecycled) return
        handOff(frameId, edit, result)
    }

    private fun finishClose(token: Long) {
        if (token != closeToken) return
        state.update { it.copy(tool = EditTool.None, draft = EditDraft(), panelPx = 0, sourceW = 0, sourceH = 0, toolBusy = false, discardPrompt = false) }
    }

    fun release() {
        generation++
        raw?.takeUnless { it.isRecycled }?.recycle()
        raw = null
        retired?.takeUnless { it.isRecycled }?.recycle()
        retired = null
    }

    internal fun update(op: (EditDraft) -> EditDraft) = state.update { if (it.toolBusy) it else it.copy(draft = op(it.draft)) }

    private val renderLock = Mutex()

    private fun renderFailed(error: Throwable, token: Long, base: Bitmap) {
        logFailure("ViewerToolRender", error)
        if (token != generation || raw !== base) return
        state.update { it.copy(message = FrameStrings.viewerLoadFailed(), busyMessage = null, messageSaved = null, messageIcon = null) }
    }

    internal fun rerender() {
        val base = raw ?: return
        val token = ++generation
        val edit = state.value.canvasEdit()
        scope.launch {
            val shown = try {
                renderLock.withLock {
                    if (token != generation || raw !== base) return@launch
                    withContext(Dispatchers.Default) { BitmapEditRenderer.renderCopy(base, edit) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                renderFailed(e, token, base)
                return@launch
            } catch (e: OutOfMemoryError) {
                renderFailed(e, token, base)
                return@launch
            }
            if (token != generation || raw !== base) {
                shown.recycle()
                return@launch
            }
            val old = state.value.bitmap
            state.update { it.copy(bitmap = shown) }
            retired?.takeUnless { it.isRecycled }?.recycle()
            retired = old?.takeUnless { it === base }
        }
    }
}
