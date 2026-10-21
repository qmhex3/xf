package com.frameout.engine.extract

object FfmpegFrameRules {
    private const val FALLBACK_FRAME_US = 33_333L

    fun resolvePts(ptsUs: Long, index: Int): Long =
        ptsUs.takeIf { it >= 0L } ?: (index * FALLBACK_FRAME_US)

    fun inRange(ptsUs: Long, startUs: Long, endUs: Long): Boolean = ptsUs in startUs..endUs

    fun keep(
        mode: ExtractMode,
        index: Int,
        ptsUs: Long,
        keyframe: Boolean,
        previous: FloatArray?,
        current: FloatArray?
    ): Boolean = when (mode) {
        ExtractMode.EveryFrame -> true
        is ExtractMode.EveryNFrames -> true
        is ExtractMode.EveryNMillis -> ptsUs >= 0L
        ExtractMode.KeyframesOnly -> keyframe
        is ExtractMode.FixedCount -> ptsUs >= 0L
        ExtractMode.SceneChange, ExtractMode.Slides -> SceneChangeSelector.changed(mode, previous, current)
    }
}
