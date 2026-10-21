package com.frameout.app.full

import com.frameout.codecpack.FfmpegBridge
import java.nio.ByteBuffer

object NativeFrames {
    const val MIN_COLORS = 8
    private val OPAQUE = 0xFF.toByte()

    class Decoded(val rc: Int, val ptsUs: Long, val colors: Int, val opaque: Boolean)

    fun decode(path: String, position: Int, width: Int, height: Int): Decoded {
        val buffer = ByteBuffer.allocateDirect(width * height * 4)
        val pts = LongArray(1)
        val rc = FfmpegBridge.nativeDecodeFrame(path, position, buffer, width, height, pts, BooleanArray(1), IntArray(1), LongArray(1))
        return Decoded(rc, pts[0], distinctColors(buffer), allOpaque(buffer))
    }

    fun halfFrameUs(index: LongArray): Long {
        val frames = index.size / 4
        if (frames < 2) return Long.MAX_VALUE
        return ((index[(frames - 1) * 4] - index[0]) / (frames - 1)) / 2
    }

    private fun distinctColors(buffer: ByteBuffer): Int {
        val seen = HashSet<Int>()
        val pixels = buffer.duplicate().apply { rewind() }.asIntBuffer()
        while (pixels.hasRemaining() && seen.size < MIN_COLORS) seen.add(pixels.get())
        return seen.size
    }

    private fun allOpaque(buffer: ByteBuffer): Boolean {
        for (offset in 3 until buffer.capacity() step 4) if (buffer.get(offset) != OPAQUE) return false
        return true
    }
}
