package com.frameout.codecpack

class ApngChunk(val type: String, val data: ByteArray)

class ApngRange(val offset: Long, val length: Int)

class ApngFrame(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val delayMs: Long,
    val dispose: Int,
    val blend: Int,
    val data: List<ApngRange>
)

class ApngIndex(
    val width: Int,
    val height: Int,
    val ihdr: ByteArray,
    val shared: List<ApngChunk>,
    val frames: List<ApngFrame>
) {
    fun startsUs(): LongArray {
        val out = LongArray(frames.size)
        var time = 0L
        for (i in frames.indices) {
            out[i] = time * 1000L
            time += frames[i].delayMs
        }
        return out
    }

    fun durationUs(): Long = frames.sumOf { it.delayMs } * 1000L

    companion object {
        const val DISPOSE_NONE = 0
        const val DISPOSE_BACKGROUND = 1
        const val DISPOSE_PREVIOUS = 2
        const val BLEND_SOURCE = 0
        const val BLEND_OVER = 1
    }
}
