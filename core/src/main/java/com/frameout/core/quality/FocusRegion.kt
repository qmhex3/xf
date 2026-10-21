package com.frameout.core.quality

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PixelRegion(val x: Int, val y: Int, val w: Int, val h: Int)

object FocusRegion {
    private const val PAD = 0.12f
    private const val MIN_FRACTION = 0.25f

    fun largest(boxes: List<NormBox>): NormBox? = boxes.maxByOrNull { it.width * it.height }

    fun union(boxes: List<NormBox>): NormBox? {
        if (boxes.isEmpty()) return null
        return NormBox(boxes.minOf { it.l }, boxes.minOf { it.t }, boxes.maxOf { it.r }, boxes.maxOf { it.b })
    }

    fun pixels(box: NormBox, imageWidth: Int, imageHeight: Int, aspect: Float?): PixelRegion {
        if (imageWidth <= 0 || imageHeight <= 0) return PixelRegion(0, 0, max(imageWidth, 1), max(imageHeight, 1))
        val padded = box.padded(PAD)
        val iw = imageWidth.toFloat()
        val ih = imageHeight.toFloat()
        var w = max(padded.width * iw, MIN_FRACTION * iw)
        var h = max(padded.height * ih, MIN_FRACTION * ih)
        if (aspect != null && aspect > 0f) {
            if (w / h < aspect) w = h * aspect else h = w / aspect
            if (w > iw) { w = iw; h = w / aspect }
            if (h > ih) { h = ih; w = h * aspect }
        } else {
            w = min(w, iw)
            h = min(h, ih)
        }
        val cx = (padded.l + padded.r) / 2f * iw
        val cy = (padded.t + padded.b) / 2f * ih
        val x = (cx - w / 2f).coerceIn(0f, iw - w)
        val y = (cy - h / 2f).coerceIn(0f, ih - h)
        val rw = w.roundToInt().coerceIn(1, imageWidth)
        val rh = h.roundToInt().coerceIn(1, imageHeight)
        return PixelRegion(x.toInt().coerceIn(0, imageWidth - rw), y.toInt().coerceIn(0, imageHeight - rh), rw, rh)
    }
}
