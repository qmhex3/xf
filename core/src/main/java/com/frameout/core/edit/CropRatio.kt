package com.frameout.core.edit

data class CropRect(val l: Float, val t: Float, val r: Float, val b: Float)

enum class CropPreset(val label: String, val w: Int, val h: Int) {
    Free("", 0, 0),
    Square("1:1", 1, 1),
    Portrait("4:5", 4, 5),
    Story("9:16", 9, 16),
    Wide("16:9", 16, 9)
}

object CropRatio {
    fun clamp(l: Float, t: Float, r: Float, b: Float, minSide: Float = 0.05f): CropRect {
        val left = l.coerceIn(0f, 1f - minSide)
        val top = t.coerceIn(0f, 1f - minSide)
        val right = r.coerceIn(left + minSide, 1f)
        val bottom = b.coerceIn(top + minSide, 1f)
        return CropRect(left, top, right, bottom)
    }

    fun centered(ratioW: Int, ratioH: Int, imageW: Int, imageH: Int): CropRect {
        if (imageW <= 0 || imageH <= 0) return CropRect(0f, 0f, 1f, 1f)
        val target = ratioW.toFloat() / ratioH
        val image = imageW.toFloat() / imageH
        val fracW = if (image > target) target / image else 1f
        val fracH = if (image > target) 1f else image / target
        val l = (1f - fracW) / 2f
        val t = (1f - fracH) / 2f
        return CropRect(l, t, l + fracW, t + fracH)
    }

    fun centered(preset: CropPreset, imageW: Int, imageH: Int): CropRect =
        if (preset == CropPreset.Free) CropRect(0f, 0f, 1f, 1f) else centered(preset.w, preset.h, imageW, imageH)
}
