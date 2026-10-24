package com.frameout.app.viewer

import android.graphics.Bitmap
import android.net.Uri
import com.frameout.core.edit.BitmapEditRenderer
import com.frameout.core.edit.CoverStyle
import com.frameout.core.edit.CropPreset
import com.frameout.core.edit.CropRatio
import com.frameout.core.edit.EditHistoryRepository
import com.frameout.core.edit.EditMark
import com.frameout.core.edit.FilterPresets
import com.frameout.core.edit.FrameEdit
import com.frameout.core.edit.TextMark
import com.frameout.core.edit.ViewGeometry
import com.frameout.core.util.EditStrings
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException
import com.frameout.core.edit.toEdit
import com.frameout.core.edit.toEntity
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
    private val state: MutableStateFlow<ViewerState>,
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
    private var viewW = 0f
    private var viewH = 0f

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
    fun setStrength(value: Float) = update { it.copy(strength = value) }
    fun setShape(value: CoverShape) = update { it.copy(shape = value) }
    fun setCoverSize(value: Float) = update { it.copy(coverSize = value) }
    fun setPickerOpen(open: Boolean) = update { it.copy(pickerOpen = open, pickerTarget = ColorTarget.Brush) }
    fun openPicker(target: ColorTarget) = update { it.copy(pickerOpen = true, pickerTarget = target) }
    fun setViewport(width: Float, height: Float) { viewW = width; viewH = height }

    fun setPickerColor(value: Int) = when (state.value.draft.pickerTarget) {
        ColorTarget.Brush -> setColor(value)
        ColorTarget.TextFill -> editText { it.copy(color = value) }
        ColorTarget.TextOutline -> editText { it.copy(outlineColor = value, outline = if (it.outline == 0f) 0.5f else it.outline) }
        ColorTarget.TextBackground -> editText { it.copy(background = value or 0xFF000000.toInt()) }
    }
    fun setBrightness(value: Float) = update { it.copy(brightness = value) }
    fun setContrast(value: Float) = update { it.copy(contrast = value) }
    fun setSaturation(value: Float) = update { it.copy(saturation = value) }
    fun setFilter(id: String) = update { it.copy(filter = id, filterStrength = if (id == it.filter) it.filterStrength else FilterPresets.DEFAULT_STRENGTH) }
    fun setFilterStrength(value: Float) = update { it.copy(filterStrength = value.coerceIn(0f, 1f)) }
    fun resetFilter() = update { it.copy(filter = FilterPresets.NONE, filterStrength = FilterPresets.DEFAULT_STRENGTH) }
    fun setColor(value: Int) = update { shapes.restyle(it.copy(color = value)) { m -> m.copy(color = value) } }
    fun setSize(value: Float) = update { it.copy(size = value) }
    fun setStyle(value: CoverStyle) = update { it.copy(style = value) }
    fun resetAdjust() = update { it.copy(brightness = 0f, contrast = 1f, saturation = 1f) }

    fun addMark(mark: EditMark) {
        if (state.value.toolBusy) return
        val stored = state.value.markSpace()?.toSource(mark) ?: mark
        update { it.copy(marks = it.marks + stored) }
        rerender()
    }

    fun undoMark() {
        if (state.value.toolBusy) return
        if (state.value.draft.marks.isEmpty()) return
        update { it.copy(marks = it.marks.dropLast(1), selected = if (it.selected >= it.marks.size - 1) -1 else it.selected, selectedShape = -1) }
        rerender()
    }

    fun addText(content: String = EditStrings.textDefault()) {
        val current = state.value
        val bitmap = current.bitmap ?: return
        val geo = ViewGeometry(bitmap.width, bitmap.height, viewW.coerceAtLeast(1f), viewH.coerceAtLeast(1f))
        val t = current.transform()
        val x = geo.toNormX(0f, t).coerceIn(0.05f, 0.95f)
        val y = geo.toNormY(0f, t).coerceIn(0.05f, 0.95f)
        val space = current.markSpace()
        update { d ->
            val cleaned = dropBlankTexts(d, -1)
            val shown = cleaned.text.copy(text = content, x = x, y = y, rotation = 0f)
            val mark = space?.textToSource(shown) ?: shown
            cleaned.copy(marks = cleaned.marks + mark, selected = cleaned.marks.size)
        }
    }

    fun selectText(index: Int) = update { dropBlankTexts(it, index) }

    fun deselectText() = update { dropBlankTexts(it, -1) }

    fun deleteSelectedText() = update { d ->
        val i = d.selected
        if (d.selectedText == null) d else d.copy(marks = d.marks.filterIndexed { n, _ -> n != i }, selected = -1)
    }

    fun setTextContent(content: String) {
        if (state.value.draft.selectedText == null) {
            if (content.isNotBlank()) addText(content)
            return
        }
        updateSelected { it.copy(text = content) }
    }

    fun editText(op: (TextMark) -> TextMark) = update { d ->
        val template = op(d.text).copy(text = "", x = 0.5f, y = 0.5f, rotation = 0f)
        val sel = d.selectedText ?: return@update d.copy(text = template)
        val space = state.value.markSpace()
        val next = op(space?.textToOutput(sel) ?: sel)
        d.copy(marks = d.marks.replaceAt(d.selected, space?.textToSource(next) ?: next), text = template)
    }

    fun moveText(dx: Float, dy: Float) = updateSelected { it.copy(x = (it.x + dx).coerceIn(0f, 1f), y = (it.y + dy).coerceIn(0f, 1f)) }

    fun setTextGeometry(size: Float, rotation: Float) = updateSelected {
        it.copy(size = size.coerceIn(TextMark.MIN_SIZE, TextMark.MAX_SIZE), rotation = normalizeAngle(rotation))
    }

    fun scaleRotateText(factor: Float, degrees: Float) = updateSelected {
        it.copy(size = (it.size * factor).coerceIn(TextMark.MIN_SIZE, TextMark.MAX_SIZE), rotation = normalizeAngle(it.rotation + degrees))
    }

    private fun updateSelected(op: (TextMark) -> TextMark) = update { d ->
        val sel = d.selectedText ?: return@update d
        val space = state.value.markSpace()
        val next = op(space?.textToOutput(sel) ?: sel)
        d.copy(marks = d.marks.replaceAt(d.selected, space?.textToSource(next) ?: next), text = d.text.copy(size = next.size))
    }

    private fun dropBlankTexts(d: EditDraft, keep: Int): EditDraft {
        var target = keep
        val kept = ArrayList<EditMark>(d.marks.size)
        d.marks.forEachIndexed { i, m ->
            val drop = m is TextMark && m.blank && i != keep
            if (!drop) kept += m else if (i < keep) target--
        }
        val chosen = kept.getOrNull(target) as? TextMark
        val space = state.value.markSpace()
        val shown = chosen?.let { space?.textToOutput(it) ?: it }
        val template = shown?.copy(text = "", x = 0.5f, y = 0.5f, rotation = 0f) ?: d.text
        return d.copy(marks = kept, selected = if (chosen != null) target else -1, text = template)
    }

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

    private fun update(op: (EditDraft) -> EditDraft) = state.update { if (it.toolBusy) it else it.copy(draft = op(it.draft)) }

    private val renderLock = Mutex()

    private fun renderFailed(error: Throwable, token: Long, base: Bitmap) {
        logFailure("ViewerToolRender", error)
        if (token != generation || raw !== base) return
        state.update { it.copy(message = FrameStrings.viewerLoadFailed(), busyMessage = null, messageSaved = null, messageIcon = null) }
    }

    private fun rerender() {
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
