package com.frameout.engine.player

object PlaybackSpeeds {
    val values: List<Float> = listOf(0.25f, 0.5f, 1f, 1.5f, 2f)
    const val NORMAL = 1f

    fun next(current: Float): Float = values.firstOrNull { it > current + EPSILON } ?: values.first()

    fun label(speed: Float): String {
        val text = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')
        return text + "\u00D7"
    }

    private const val EPSILON = 0.001f
}
