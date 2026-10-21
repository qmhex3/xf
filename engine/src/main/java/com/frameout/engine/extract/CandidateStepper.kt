package com.frameout.engine.extract

internal class CandidateStepper(private val stepUs: Long) {
    private var next = Long.MIN_VALUE

    fun accept(ptsUs: Long, keyframe: Boolean): Boolean {
        if (next == Long.MIN_VALUE) next = ptsUs
        if (ptsUs < next && !keyframe) return false
        next = if (ptsUs > Long.MAX_VALUE - stepUs) Long.MAX_VALUE else ptsUs + stepUs
        return true
    }

    companion object {
        fun forMode(mode: ExtractMode): CandidateStepper? = when (mode) {
            ExtractMode.SceneChange -> CandidateStepper(SceneChangeSelector.SCENE_STEP_US)
            ExtractMode.Slides -> CandidateStepper(SceneChangeSelector.SLIDES_STEP_US)
            else -> null
        }
    }
}
