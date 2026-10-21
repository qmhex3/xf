package com.frameout.engine.extract
import android.graphics.Bitmap
import com.frameout.core.quality.LumaGrid
import com.frameout.core.quality.SlideSignature
object SceneChangeSelector {
    const val SCENE_STEP_US = 50_000L
    const val SLIDES_STEP_US = 250_000L
    fun signature(mode: ExtractMode, bitmap: Bitmap): FloatArray? = when (mode) {
        ExtractMode.SceneChange -> SceneDetector.signature(bitmap)
        ExtractMode.Slides -> SlideSignature.of(bitmap)
        else -> null
    }
    fun signature(mode: ExtractMode, width: Int, height: Int, luma: LumaGrid): FloatArray? = when (mode) {
        ExtractMode.SceneChange -> SceneDetector.signature(width, height, luma)
        ExtractMode.Slides -> SlideSignature.of(width, height, luma)
        else -> null
    }
    fun usesLuma(mode: ExtractMode): Boolean = mode == ExtractMode.SceneChange || mode == ExtractMode.Slides
    fun changed(mode: ExtractMode, prev: FloatArray?, curr: FloatArray?): Boolean {
        if (curr == null || prev == null) return true
        return if (mode == ExtractMode.Slides) SlideSignature.differs(prev, curr) else SceneDetector.changed(prev, curr)
    }
    fun carry(mode: ExtractMode, previous: FloatArray?, current: FloatArray?): FloatArray? =
        if (mode == ExtractMode.Slides) previous else SceneDetector.carry(previous, current)
}
