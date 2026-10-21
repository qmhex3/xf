package com.frameout.core.collage

import kotlin.math.max
import kotlin.math.roundToInt

const val COLLAGE_MAX_ZOOM = 5f

data class CellCrop(val scale: Float = 1f, val offsetX: Float = 0f, val offsetY: Float = 0f) {
    val isDefault: Boolean get() = scale == 1f && offsetX == 0f && offsetY == 0f
}

data class SourceWindow(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class PixelWindow(val left: Int, val top: Int, val right: Int, val bottom: Int)

class CropGeometry(srcWidth: Float, srcHeight: Float, cellWidth: Float, cellHeight: Float) {
    private val sw = srcWidth.coerceAtLeast(1f)
    private val sh = srcHeight.coerceAtLeast(1f)
    private val cw = cellWidth.coerceAtLeast(1f)
    private val ch = cellHeight.coerceAtLeast(1f)
    private val base = max(cw / sw, ch / sh)

    fun clamp(crop: CellCrop): CellCrop {
        val scale = crop.scale.coerceIn(1f, COLLAGE_MAX_ZOOM)
        val maxX = max(0f, (sw * base * scale / cw - 1f) / 2f)
        val maxY = max(0f, (sh * base * scale / ch - 1f) / 2f)
        return CellCrop(scale, crop.offsetX.coerceIn(-maxX, maxX), crop.offsetY.coerceIn(-maxY, maxY))
    }

    fun window(crop: CellCrop): SourceWindow {
        val c = clamp(crop)
        val s = base * c.scale
        val left = sw / 2f - cw / (2f * s) - c.offsetX * cw / s
        val top = sh / 2f - ch / (2f * s) - c.offsetY * ch / s
        return SourceWindow(left, top, left + cw / s, top + ch / s)
    }

    fun pixelWindow(crop: CellCrop): PixelWindow {
        val w = window(crop)
        val maxW = sw.toInt().coerceAtLeast(1)
        val maxH = sh.toInt().coerceAtLeast(1)
        val left = w.left.roundToInt().coerceIn(0, maxW - 1)
        val top = w.top.roundToInt().coerceIn(0, maxH - 1)
        return PixelWindow(left, top, w.right.roundToInt().coerceIn(left + 1, maxW), w.bottom.roundToInt().coerceIn(top + 1, maxH))
    }

    fun transform(crop: CellCrop, panX: Float, panY: Float, zoom: Float, focusX: Float, focusY: Float): CellCrop {
        val from = clamp(crop)
        val factor = (from.scale * zoom).coerceIn(1f, COLLAGE_MAX_ZOOM) / from.scale
        val tx = factor * from.offsetX * cw + (1f - factor) * (focusX - cw / 2f) + panX
        val ty = factor * from.offsetY * ch + (1f - factor) * (focusY - ch / 2f) + panY
        return clamp(CellCrop(from.scale * factor, tx / cw, ty / ch))
    }
}
