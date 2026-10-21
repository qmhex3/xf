package com.frameout.app.viewer

import com.frameout.core.edit.CropRect
import com.frameout.core.edit.MarkSpace
import com.frameout.core.edit.ShapeMark
import com.frameout.core.edit.TextMark

internal fun ViewerState.sourceCrop() = CropRect(cropL, cropT, cropR, cropB)

internal fun ViewerState.displayCrop(): CropRect =
    GeometryMath.sourceToDisplay(sourceCrop(), draft.rotation, draft.flipH, draft.flipV)

internal fun ViewerState.withDisplayCrop(display: CropRect): ViewerState {
    val s = GeometryMath.displayToSource(display, draft.rotation, draft.flipH, draft.flipV)
    return copy(cropL = s.l, cropT = s.t, cropR = s.r, cropB = s.b)
}

internal fun ViewerState.withoutCrop() = copy(cropL = 0f, cropT = 0f, cropR = 1f, cropB = 1f)

internal fun ViewerState.markSpace(): MarkSpace? =
    if (sourceW <= 0 || sourceH <= 0) null
    else MarkSpace(sourceW, sourceH, draft.rotation, draft.flipH, draft.flipV, cropL, cropT, cropR, cropB)

internal fun ViewerState.displayDraft(): EditDraft {
    val space = markSpace() ?: return draft
    return draft.copy(marks = draft.marks.map {
        when (it) {
            is TextMark -> space.textToOutput(it)
            is ShapeMark -> space.toOutput(it)
            else -> it
        }
    })
}

internal fun ViewerState.shapesLive(): Boolean = tool == EditTool.Draw && draft.drawMode == DrawMode.Shapes
