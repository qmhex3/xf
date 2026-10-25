package com.frameout.engine.player

import kotlin.math.roundToInt

internal object HdrPreviewPolicy {
    const val MAX_EDGE = 1920
    const val DEBOUNCE_MS = 120L

    fun wantsStill(
        enabled: Boolean,
        sourceHdr: Boolean,
        windowHdr: Boolean,
        animated: Boolean,
        playing: Boolean,
        scrubbing: Boolean,
        ready: Boolean
    ): Boolean = enabled && sourceHdr && !windowHdr && !animated && !playing && !scrubbing && ready

    fun scaledSize(width: Int, height: Int, maxEdge: Int = MAX_EDGE): Pair<Int, Int> {
        val longest = maxOf(width, height)
        if (width <= 0 || height <= 0 || longest <= maxEdge) return width to height
        val k = maxEdge.toDouble() / longest
        return maxOf(1, (width * k).roundToInt()) to maxOf(1, (height * k).roundToInt())
    }
}
