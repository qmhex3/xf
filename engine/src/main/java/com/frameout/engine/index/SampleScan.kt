package com.frameout.engine.index

import com.frameout.core.db.VideoIndexEntity

internal class SampleScan {
    private var pts = LongArray(INITIAL)
    private var keys = BooleanArray(INITIAL)
    var size = 0
        private set

    fun add(ptsUs: Long, keyframe: Boolean) {
        if (size == pts.size) {
            pts = pts.copyOf(size * 2)
            keys = keys.copyOf(size * 2)
        }
        pts[size] = ptsUs
        keys[size] = keyframe
        size++
    }

    suspend fun forEachDisplay(emit: suspend (VideoIndexEntity) -> Unit) {
        val ids = order()
        var lastKey = -1
        var lastKeyPts = -1L
        for (rank in 0 until size) {
            val source = ids[rank]
            if (keys[source]) { lastKey = rank; lastKeyPts = pts[source] }
            emit(VideoIndexEntity(projectId = 0L, sampleIndex = rank, ptsUs = pts[source], isKeyframe = keys[source], nearestKeyframeIndex = lastKey, nearestKeyframePtsUs = lastKeyPts))
        }
    }

    private fun order(): IntArray {
        val ids = IntArray(size) { it }
        if (isAscending()) return ids
        var from = ids
        var into = IntArray(size)
        var width = 1
        while (width < size) {
            mergePass(from, into, width)
            val swap = from
            from = into
            into = swap
            width *= 2
        }
        return from
    }

    private fun mergePass(from: IntArray, into: IntArray, width: Int) {
        var start = 0
        while (start < size) {
            val middle = minOf(start + width, size)
            val end = minOf(start + 2 * width, size)
            merge(from, into, start, middle, end)
            start = end
        }
    }

    private fun merge(from: IntArray, into: IntArray, start: Int, middle: Int, end: Int) {
        var left = start
        var right = middle
        var out = start
        while (left < middle && right < end) {
            into[out++] = if (pts[from[right]] < pts[from[left]]) from[right++] else from[left++]
        }
        while (left < middle) into[out++] = from[left++]
        while (right < end) into[out++] = from[right++]
    }

    private fun isAscending(): Boolean {
        for (i in 1 until size) if (pts[i] < pts[i - 1]) return false
        return true
    }

    private companion object {
        const val INITIAL = 4096
    }
}
