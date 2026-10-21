package com.frameout.app.viewer

import com.frameout.core.edit.MarkSpace
import com.frameout.core.edit.ShapeFill
import com.frameout.core.edit.ShapeKind
import com.frameout.core.edit.ShapeMark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val DUPLICATE_OFFSET = 0.04f

internal class ShapeEditor(
    private val state: MutableStateFlow<ViewerState>,
    private val rerender: () -> Unit
) {
    private fun update(op: (EditDraft) -> EditDraft) = state.update { it.copy(draft = op(it.draft)) }

    private fun space(): MarkSpace? = state.value.markSpace()

    private fun shown(raw: ShapeMark): ShapeMark = space()?.toOutput(raw) as? ShapeMark ?: raw

    private fun stored(mark: ShapeMark): ShapeMark = space()?.toSource(mark) as? ShapeMark ?: mark

    fun setMode(mode: DrawMode) {
        if (state.value.draft.drawMode == mode) return
        update { it.copy(drawMode = mode, selectedShape = -1, pickerOpen = false) }
        rerender()
    }

    fun setKind(kind: ShapeKind) = update { restyle(it.copy(shapeKind = kind)) { m -> m.copy(kind = kind) } }

    fun setFill(fill: ShapeFill) = update { restyle(it.copy(shapeFill = fill)) { m -> m.copy(fill = fill) } }

    fun setDash(dash: Boolean) = update { restyle(it.copy(shapeDash = dash)) { m -> m.copy(dash = dash) } }

    fun setOpacity(value: Float) {
        val v = value.coerceIn(ShapeMark.MIN_OPACITY, 1f)
        update { restyle(it.copy(shapeOpacity = v)) { m -> m.copy(opacity = v) } }
    }

    fun setWidth(value: Float) {
        val v = value.coerceIn(ShapeMark.MIN_WIDTH, ShapeMark.MAX_WIDTH)
        update { restyle(it.copy(shapeWidth = v)) { m -> m.copy(width = v) } }
    }

    fun add(mark: ShapeMark) {
        val keep = stored(mark)
        update { it.copy(marks = it.marks + keep, selectedShape = it.marks.size) }
    }

    fun replace(index: Int, mark: ShapeMark) {
        val keep = stored(mark)
        update { d -> if (d.marks.getOrNull(index) is ShapeMark) d.copy(marks = d.marks.replaceAt(index, keep)) else d }
    }

    fun select(index: Int) = update { d ->
        val raw = d.marks.getOrNull(index) as? ShapeMark ?: return@update d.copy(selectedShape = -1)
        val m = shown(raw)
        d.copy(
            selectedShape = index, shapeKind = m.kind, color = m.color, shapeWidth = m.width,
            shapeFill = m.fill, shapeDash = m.dash, shapeOpacity = m.opacity
        )
    }

    fun deleteSelected() = update { d ->
        if (d.shapeSelection == null) d else d.copy(marks = d.marks.filterIndexed { n, _ -> n != d.selectedShape }, selectedShape = -1)
    }

    fun duplicateSelected() = update { d ->
        val raw = d.shapeSelection ?: return@update d
        val m = shown(raw)
        val dx = DUPLICATE_OFFSET.coerceAtMost(1f - maxOf(m.x1, m.x2)).coerceAtLeast(-minOf(m.x1, m.x2))
        val dy = DUPLICATE_OFFSET.coerceAtMost(1f - maxOf(m.y1, m.y2)).coerceAtLeast(-minOf(m.y1, m.y2))
        val copy = m.copy(x1 = m.x1 + dx, y1 = m.y1 + dy, x2 = m.x2 + dx, y2 = m.y2 + dy, number = nextNumber(d))
        d.copy(marks = d.marks + stored(copy), selectedShape = d.marks.size)
    }

    fun restyle(draft: EditDraft, op: (ShapeMark) -> ShapeMark): EditDraft {
        if (draft.drawMode != DrawMode.Shapes) return draft
        val i = draft.selectedShape
        val raw = draft.marks.getOrNull(i) as? ShapeMark ?: return draft
        return draft.copy(marks = draft.marks.replaceAt(i, stored(op(shown(raw)))))
    }
}

internal fun nextNumber(draft: EditDraft): Int =
    draft.marks.count { it is ShapeMark && it.kind == ShapeKind.Number } + 1
