package com.frameout.engine.extract

object FfmpegSelection {
    fun encodeMode(mode: ExtractMode): String = when (mode) {
        ExtractMode.EveryFrame -> "EveryFrame"
        is ExtractMode.EveryNFrames -> "EveryNFrames:${mode.n}"
        is ExtractMode.EveryNMillis -> "EveryNMillis:${mode.millis}"
        ExtractMode.KeyframesOnly -> "KeyframesOnly"
        is ExtractMode.FixedCount -> "FixedCount:${mode.count}"
        ExtractMode.SceneChange -> "SceneChange"
        ExtractMode.Slides -> "Slides"
    }
}
