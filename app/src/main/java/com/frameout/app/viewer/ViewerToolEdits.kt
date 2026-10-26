package com.frameout.app.viewer

import com.frameout.core.edit.CoverStyle
import com.frameout.core.edit.EditMark
import com.frameout.core.edit.FilterPresets
import com.frameout.core.edit.TextMark
import com.frameout.core.edit.ViewGeometry
import com.frameout.core.util.EditStrings
import kotlinx.coroutines.flow.update

internal fun ViewerToolEditor.setStrength(value: Float) = update { it.copy(strength = value) }
internal fun ViewerToolEditor.setShape(value: CoverShape) = update { it.copy(shape = value) }
internal fun ViewerToolEditor.setCoverSize(value: Float) = update { it.copy(coverSize = value) }
internal fun ViewerToolEditor.setPickerOpen(open: Boolean) = update { it.copy(pickerOpen = open, pickerTarget = ColorTarget.Brush) }
internal fun ViewerToolEditor.openPicker(target: ColorTarget) = update { it.copy(pickerOpen = true, pickerTarget = target) }
internal fun ViewerToolEditor.setPickerColor(value: Int) = when (state.value.draft.pickerTarget) {
    ColorTarget.Brush -> setColor(value)
    ColorTarget.TextFill -> editText { it.copy(color = value) }
    ColorTarget.TextOutline -> editText { it.copy(outlineColor = value, outline = if (it.outline == 0f) 0.5f else it.outline) }
    ColorTarget.TextBackground -> editText { it.copy(background = value or 0xFF000000.toInt()) }
}
internal fun ViewerToolEditor.setBrightness(value: Float) = update { it.copy(brightness = value) }
internal fun ViewerToolEditor.setContrast(value: Float) = update { it.copy(contrast = value) }
internal fun ViewerToolEditor.setSaturation(value: Float) = update { it.copy(saturation = value) }
internal fun ViewerToolEditor.setFilter(id: String) = update { it.copy(filter = id, filterStrength = if (id == it.filter) it.filterStrength else FilterPresets.DEFAULT_STRENGTH) }
internal fun ViewerToolEditor.setFilterStrength(value: Float) = update { it.copy(filterStrength = value.coerceIn(0f, 1f)) }
internal fun ViewerToolEditor.resetFilter() = update { it.copy(filter = FilterPresets.NONE, filterStrength = FilterPresets.DEFAULT_STRENGTH) }
internal fun ViewerToolEditor.setColor(value: Int) = update { shapes.restyle(it.copy(color = value)) { m -> m.copy(color = value) } }
internal fun ViewerToolEditor.setSize(value: Float) = update { it.copy(size = value) }
internal fun ViewerToolEditor.setStyle(value: CoverStyle) = update { it.copy(style = value) }
internal fun ViewerToolEditor.resetAdjust() = update { it.copy(brightness = 0f, contrast = 1f, saturation = 1f) }
internal fun ViewerToolEditor.addMark(mark: EditMark) {
    if (state.value.toolBusy) return
    val stored = state.value.markSpace()?.toSource(mark) ?: mark
    update { it.copy(marks = it.marks + stored) }
    rerender()
}

internal fun ViewerToolEditor.undoMark() {
    if (state.value.toolBusy) return
    if (state.value.draft.marks.isEmpty()) return
    update { it.copy(marks = it.marks.dropLast(1), selected = if (it.selected >= it.marks.size - 1) -1 else it.selected, selectedShape = -1) }
    rerender()
}

internal fun ViewerToolEditor.addText(content: String = EditStrings.textDefault()) {
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

internal fun ViewerToolEditor.selectText(index: Int) = update { dropBlankTexts(it, index) }

internal fun ViewerToolEditor.deselectText() = update { dropBlankTexts(it, -1) }

internal fun ViewerToolEditor.deleteSelectedText() = update { d ->
    val i = d.selected
    if (d.selectedText == null) d else d.copy(marks = d.marks.filterIndexed { n, _ -> n != i }, selected = -1)
}

internal fun ViewerToolEditor.setTextContent(content: String) {
    if (state.value.draft.selectedText == null) {
        if (content.isNotBlank()) addText(content)
        return
    }
    updateSelected { it.copy(text = content) }
}

internal fun ViewerToolEditor.editText(op: (TextMark) -> TextMark) = update { d ->
    val template = op(d.text).copy(text = "", x = 0.5f, y = 0.5f, rotation = 0f)
    val sel = d.selectedText ?: return@update d.copy(text = template)
    val space = state.value.markSpace()
    val next = op(space?.textToOutput(sel) ?: sel)
    d.copy(marks = d.marks.replaceAt(d.selected, space?.textToSource(next) ?: next), text = template)
}

internal fun ViewerToolEditor.moveText(dx: Float, dy: Float) = updateSelected { it.copy(x = (it.x + dx).coerceIn(0f, 1f), y = (it.y + dy).coerceIn(0f, 1f)) }

internal fun ViewerToolEditor.setTextGeometry(size: Float, rotation: Float) = updateSelected {
    it.copy(size = size.coerceIn(TextMark.MIN_SIZE, TextMark.MAX_SIZE), rotation = normalizeAngle(rotation))
}

internal fun ViewerToolEditor.scaleRotateText(factor: Float, degrees: Float) = updateSelected {
    it.copy(size = (it.size * factor).coerceIn(TextMark.MIN_SIZE, TextMark.MAX_SIZE), rotation = normalizeAngle(it.rotation + degrees))
}

internal fun ViewerToolEditor.updateSelected(op: (TextMark) -> TextMark) = update { d ->
    val sel = d.selectedText ?: return@update d
    val space = state.value.markSpace()
    val next = op(space?.textToOutput(sel) ?: sel)
    d.copy(marks = d.marks.replaceAt(d.selected, space?.textToSource(next) ?: next), text = d.text.copy(size = next.size))
}

internal fun ViewerToolEditor.dropBlankTexts(d: EditDraft, keep: Int): EditDraft {
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
