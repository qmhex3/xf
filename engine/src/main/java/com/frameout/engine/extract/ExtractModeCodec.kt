package com.frameout.engine.extract

object ExtractModeCodec {
    fun encode(mode: ExtractMode): String = when (mode) {
        ExtractMode.EveryFrame -> "EveryFrame"
        is ExtractMode.EveryNFrames -> "EveryNFrames:${mode.n.coerceAtLeast(1)}"
        is ExtractMode.EveryNMillis -> "EveryNMillis:${mode.millis.coerceAtLeast(1)}"
        ExtractMode.KeyframesOnly -> "KeyframesOnly"
        is ExtractMode.FixedCount -> "FixedCount:${mode.count.coerceAtLeast(1)}"
        ExtractMode.SceneChange -> "SceneChange"
        ExtractMode.Slides -> "Slides"
    }

    fun decode(value: String?): ExtractMode = when {
        value.isNullOrBlank() || value == "EveryFrame" || value == "every_frame" -> ExtractMode.EveryFrame
        value.startsWith("EveryNFrames:") -> ExtractMode.EveryNFrames(value.substringAfter(':').toIntOrNull()?.coerceAtLeast(1) ?: 1)
        value.startsWith("EveryNMillis:") -> ExtractMode.EveryNMillis(value.substringAfter(':').toLongOrNull()?.coerceAtLeast(1) ?: 1000L)
        value == "KeyframesOnly" || value == "keyframes" -> ExtractMode.KeyframesOnly
        value.startsWith("FixedCount:") -> ExtractMode.FixedCount(value.substringAfter(':').toIntOrNull()?.coerceAtLeast(1) ?: 10)
        value == "SceneChange" || value == "scene" -> ExtractMode.SceneChange
        value == "Slides" || value == "slides" -> ExtractMode.Slides
        value == "every_5" -> ExtractMode.EveryNFrames(5)
        value == "every_1000ms" -> ExtractMode.EveryNMillis(1000)
        value == "fixed_30" -> ExtractMode.FixedCount(30)
        else -> ExtractMode.EveryFrame
    }
}
