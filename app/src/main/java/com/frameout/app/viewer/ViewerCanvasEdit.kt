package com.frameout.app.viewer

import com.frameout.core.edit.FrameEdit
import com.frameout.core.edit.ShapeMark
import com.frameout.core.edit.TextMark

internal fun ViewerState.canvasEdit(): FrameEdit {
    val d = draft
    val colorTool = tool.isColorTool
    val marks = when {
        colorTool -> emptyList()
        tool == EditTool.Text -> d.marks.filter { it !is TextMark }
        shapesLive() -> d.marks.filter { it !is ShapeMark }
        else -> d.marks
    }
    val frameId = current?.id ?: 0L
    val geometry = when (tool.canvasSpace) {
        0 -> FrameEdit(frameId, marks = marks)
        1 -> FrameEdit(frameId, rotation = d.rotation, flipH = d.flipH, flipV = d.flipV, marks = marks)
        else -> FrameEdit(
            frameId, rotation = d.rotation, flipH = d.flipH, flipV = d.flipV,
            cropL = cropL, cropT = cropT, cropR = cropR, cropB = cropB, marks = marks
        )
    }
    if (colorTool) return geometry
    return geometry.copy(
        brightness = d.brightness, contrast = d.contrast, saturation = d.saturation,
        filter = d.filter, filterStrength = d.filterStrength
    )
}
