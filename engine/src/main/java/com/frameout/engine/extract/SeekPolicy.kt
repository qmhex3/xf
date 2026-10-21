package com.frameout.engine.extract

import com.frameout.engine.index.SampleInfo

internal object SeekPolicy {
    const val FORWARD_SLACK = 6

    fun needsSeek(sample: SampleInfo, position: Int): Boolean =
        position < 0 || sample.index <= position || sample.nearestKeyframeIndex > position + 1 + FORWARD_SLACK
}
