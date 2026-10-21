package com.frameout.engine.extract

internal object SpaceEstimate {
    private const val SCENE_KEEP_RATIO = 8
    private const val SLIDES_KEEP_RATIO = 20

    fun frames(mode: ExtractMode, candidates: Int): Int {
        val n = candidates.coerceAtLeast(1)
        return when (mode) {
            ExtractMode.SceneChange -> divUp(n, SCENE_KEEP_RATIO)
            ExtractMode.Slides -> divUp(n, SLIDES_KEEP_RATIO)
            else -> n
        }
    }

    private fun divUp(a: Int, b: Int): Int = ((a.toLong() + b - 1) / b).toInt().coerceAtLeast(1)
}
