package com.frameout.engine.decode

object KeyframeHintPolicy {
    const val MIN_GAP = 8

    fun needed(lastIndex: Int, targetIndex: Int, minGap: Int = MIN_GAP): Boolean {
        if (lastIndex < 0) return targetIndex > minGap
        if (targetIndex <= lastIndex) return true
        return targetIndex - lastIndex > minGap
    }
}
