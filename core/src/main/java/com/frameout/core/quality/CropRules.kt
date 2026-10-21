package com.frameout.core.quality

object CropRules {
    const val MIN_CROP_SIDE = 96

    class Rect(val x: Int, val y: Int, val width: Int, val height: Int)

    fun rect(box: NormBox, imageWidth: Int, imageHeight: Int, minSide: Int = MIN_CROP_SIDE): Rect? {
        if (imageWidth <= 0 || imageHeight <= 0) return null
        val x = (box.l * imageWidth).toInt().coerceIn(0, imageWidth - 1)
        val y = (box.t * imageHeight).toInt().coerceIn(0, imageHeight - 1)
        val width = (box.r * imageWidth).toInt().coerceIn(x + 1, imageWidth) - x
        val height = (box.b * imageHeight).toInt().coerceIn(y + 1, imageHeight) - y
        return if (width < minSide || height < minSide) null else Rect(x, y, width, height)
    }
}
