package com.frameout.engine.index

data class SampleInfo(
    val index: Int,
    val ptsUs: Long,
    val isKeyframe: Boolean,
    val nearestKeyframeIndex: Int
)
