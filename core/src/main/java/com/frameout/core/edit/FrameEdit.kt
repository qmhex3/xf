package com.frameout.core.edit

data class FrameEdit(
    val frameId: Long,
    val rotation: Int = 0,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val cropL: Float = 0f,
    val cropT: Float = 0f,
    val cropR: Float = 1f,
    val cropB: Float = 1f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val marks: List<EditMark> = emptyList(),
    val filter: String = FilterPresets.NONE,
    val filterStrength: Float = FilterPresets.DEFAULT_STRENGTH
) {
    val hasFilter: Boolean
        get() = filter != FilterPresets.NONE && filterStrength > 0f

    val hasAdjust: Boolean
        get() = brightness != 0f || contrast != 1f || saturation != 1f || hasFilter

    val isIdentity: Boolean
        get() = rotation == 0 && !flipH && !flipV &&
            cropL == 0f && cropT == 0f && cropR == 1f && cropB == 1f &&
            !hasAdjust && marks.isEmpty()

    fun rotate90(): FrameEdit = copy(rotation = (rotation + 90) % 360)
    fun toggleFlipH(): FrameEdit = copy(flipH = !flipH)
    fun toggleFlipV(): FrameEdit = copy(flipV = !flipV)
}
