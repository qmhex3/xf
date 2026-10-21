package com.frameout.core.edit

object AdjustMatrix {
    const val BRIGHTNESS_MIN = -0.5f
    const val BRIGHTNESS_MAX = 0.5f
    const val CONTRAST_MIN = 0.5f
    const val CONTRAST_MAX = 1.5f
    const val SATURATION_MIN = 0f
    const val SATURATION_MAX = 2f

    fun values(brightness: Float, contrast: Float, saturation: Float): FloatArray {
        val inv = 1f - saturation
        val r = 0.213f * inv
        val g = 0.715f * inv
        val b = 0.072f * inv
        val shift = (1f - contrast) * 127.5f + brightness * 255f
        return floatArrayOf(
            contrast * (r + saturation), contrast * g, contrast * b, 0f, shift,
            contrast * r, contrast * (g + saturation), contrast * b, 0f, shift,
            contrast * r, contrast * g, contrast * (b + saturation), 0f, shift,
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun values(brightness: Float, contrast: Float, saturation: Float, filter: String, filterStrength: Float): FloatArray {
        val adjust = values(brightness, contrast, saturation)
        if (filter == FilterPresets.NONE) return adjust
        return ColorOps.then(FilterPresets.matrix(filter, filterStrength), adjust)
    }
}
