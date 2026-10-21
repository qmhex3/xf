package com.frameout.engine.index

import com.frameout.codecpack.FfmpegBridge

data class FfmpegIndexEntry(
    val sampleIndex: Int,
    val ptsUs: Long,
    val isKeyframe: Boolean,
    val nearestKeyframeIndex: Int,
    val nearestKeyframePtsUs: Long
)

fun readFfmpegIndex(path: String): List<FfmpegIndexEntry> {
    val raw = FfmpegBridge.nativeFrameIndex(path) ?: return emptyList()
    if (raw.isEmpty() || raw.size % 4 != 0) return emptyList()
    val result = ArrayList<FfmpegIndexEntry>(raw.size / 4)
    var i = 0
    var sample = 0
    while (i < raw.size) {
        result += FfmpegIndexEntry(sample++, raw[i], raw[i + 1] != 0L, raw[i + 2].toInt(), raw[i + 3])
        i += 4
    }
    return result
}
