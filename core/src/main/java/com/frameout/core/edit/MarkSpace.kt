package com.frameout.core.edit

import kotlin.math.max
import kotlin.math.min

data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

fun cropPixels(width: Int, height: Int, l: Float, t: Float, r: Float, b: Float): PixelRect {
    val left = (width * l).toInt().coerceIn(0, width - 1)
    val top = (height * t).toInt().coerceIn(0, height - 1)
    val right = (width * r).toInt().coerceIn(left + 1, width)
    val bottom = (height * b).toInt().coerceIn(top + 1, height)
    return PixelRect(left, top, right, bottom)
}

class MarkSpace(
    sourceWidth: Int,
    sourceHeight: Int,
    rotation: Int = 0,
    private val flipH: Boolean = false,
    private val flipV: Boolean = false,
    cropL: Float = 0f,
    cropT: Float = 0f,
    cropR: Float = 1f,
    cropB: Float = 1f
) {
    private val sw = sourceWidth.coerceAtLeast(1)
    private val sh = sourceHeight.coerceAtLeast(1)
    private val turns = ((rotation % 360) + 360) % 360
    private val crop = cropPixels(sw, sh, cropL, cropT, cropR, cropB)
    private val cw = crop.width.toFloat()
    private val ch = crop.height.toFloat()
    private val swap = turns == 90 || turns == 270

    val outputWidth: Int = if (swap) crop.height else crop.width
    val outputHeight: Int = if (swap) crop.width else crop.height
    val sourceMin: Float = min(sw, sh).toFloat()
    private val outputMin: Float = min(outputWidth, outputHeight).toFloat()
    private val toOutputScale: Float = sourceMin / outputMin

    fun toOutput(p: MarkPoint): MarkPoint {
        var dx = p.x * sw - crop.left - cw / 2f
        var dy = p.y * sh - crop.top - ch / 2f
        if (flipH) dx = -dx
        if (flipV) dy = -dy
        val rx: Float
        val ry: Float
        when (turns) {
            90 -> { rx = -dy; ry = dx }
            180 -> { rx = -dx; ry = -dy }
            270 -> { rx = dy; ry = -dx }
            else -> { rx = dx; ry = dy }
        }
        return MarkPoint((rx + outputWidth / 2f) / outputWidth, (ry + outputHeight / 2f) / outputHeight)
    }

    fun toSource(p: MarkPoint): MarkPoint {
        val rx = p.x * outputWidth - outputWidth / 2f
        val ry = p.y * outputHeight - outputHeight / 2f
        var dx: Float
        var dy: Float
        when (turns) {
            90 -> { dx = ry; dy = -rx }
            180 -> { dx = -rx; dy = -ry }
            270 -> { dx = -ry; dy = rx }
            else -> { dx = rx; dy = ry }
        }
        if (flipH) dx = -dx
        if (flipV) dy = -dy
        return MarkPoint((dx + cw / 2f + crop.left) / sw, (dy + ch / 2f + crop.top) / sh)
    }

    fun toOutput(mark: EditMark): EditMark = when (mark) {
        is StrokeMark -> mark.copy(width = mark.width * toOutputScale, points = mark.points.map { toOutput(it) })
        is BrushCoverMark -> mark.copy(width = mark.width * toOutputScale, points = mark.points.map { toOutput(it) })
        is CoverMark -> rectangle(mark, toOutput(MarkPoint(mark.left, mark.top)), toOutput(MarkPoint(mark.right, mark.bottom)))
        is TextMark -> textToOutput(mark)
        is ShapeMark -> shape(mark, toOutput(MarkPoint(mark.x1, mark.y1)), toOutput(MarkPoint(mark.x2, mark.y2)), toOutputScale)
    }

    fun toSource(mark: EditMark): EditMark = when (mark) {
        is StrokeMark -> mark.copy(width = mark.width / toOutputScale, points = mark.points.map { toSource(it) })
        is BrushCoverMark -> mark.copy(width = mark.width / toOutputScale, points = mark.points.map { toSource(it) })
        is CoverMark -> rectangle(mark, toSource(MarkPoint(mark.left, mark.top)), toSource(MarkPoint(mark.right, mark.bottom)))
        is TextMark -> textToSource(mark)
        is ShapeMark -> shape(mark, toSource(MarkPoint(mark.x1, mark.y1)), toSource(MarkPoint(mark.x2, mark.y2)), 1f / toOutputScale)
    }

    fun textToOutput(mark: TextMark): TextMark {
        val at = toOutput(MarkPoint(mark.x, mark.y))
        return mark.copy(x = at.x, y = at.y, size = mark.size * toOutputScale, rotation = turnAngle(mark.rotation))
    }

    fun textToSource(mark: TextMark): TextMark {
        val at = toSource(MarkPoint(mark.x, mark.y))
        return mark.copy(x = at.x, y = at.y, size = mark.size / toOutputScale, rotation = unturnAngle(mark.rotation))
    }

    private fun shape(mark: ShapeMark, a: MarkPoint, b: MarkPoint, widthScale: Float) =
        mark.copy(x1 = a.x, y1 = a.y, x2 = b.x, y2 = b.y, width = mark.width * widthScale)

    private fun rectangle(mark: CoverMark, a: MarkPoint, b: MarkPoint) =
        mark.copy(left = min(a.x, b.x), top = min(a.y, b.y), right = max(a.x, b.x), bottom = max(a.y, b.y))

    private fun turnAngle(degrees: Float): Float {
        var a = degrees
        if (flipH) a = 180f - a
        if (flipV) a = -a
        return wrap(a + turns)
    }

    private fun unturnAngle(degrees: Float): Float {
        var a = degrees - turns
        if (flipV) a = -a
        if (flipH) a = 180f - a
        return wrap(a)
    }

    private fun wrap(degrees: Float): Float {
        var a = degrees % 360f
        if (a > 180f) a -= 360f
        if (a <= -180f) a += 360f
        return a
    }

    companion object {
        fun of(sourceWidth: Int, sourceHeight: Int, edit: FrameEdit) = MarkSpace(
            sourceWidth, sourceHeight, edit.rotation, edit.flipH, edit.flipV,
            edit.cropL, edit.cropT, edit.cropR, edit.cropB
        )
    }
}
