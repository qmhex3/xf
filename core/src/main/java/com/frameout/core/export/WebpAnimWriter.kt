package com.frameout.core.export

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

class WebpAnimWriter(file: File, private val width: Int, private val height: Int) : Closeable {
    init {
        require(width in 1..MAX_SIDE && height in 1..MAX_SIDE) { "Canvas out of range" }
    }

    private val raf = RandomAccessFile(file, "rw").also { it.setLength(0L) }
    private var frames = 0

    val frameCount: Int get() = frames

    fun begin() {
        raf.write(ascii("RIFF"))
        raf.write(le(0L, 4))
        raf.write(ascii("WEBP"))
        raf.write(ascii("VP8X"))
        raf.write(le(10L, 4))
        raf.write(byteArrayOf(ANIMATION_FLAG, 0, 0, 0))
        raf.write(le(width - 1L, 3))
        raf.write(le(height - 1L, 3))
        raf.write(ascii("ANIM"))
        raf.write(le(6L, 4))
        raf.write(le(0L, 4))
        raf.write(le(0L, 2))
    }

    fun addFrame(webp: ByteArray, durationMs: Int) {
        val data = WebpFrameChunks.extract(webp)
        val size = WebpFrameChunks.dimensions(webp)
        require(size.first == width && size.second == height) { "Frame ${size.first}x${size.second} does not match canvas ${width}x$height" }
        raf.write(ascii("ANMF"))
        raf.write(le(FRAME_HEADER + data.size.toLong(), 4))
        raf.write(le(0L, 3))
        raf.write(le(0L, 3))
        raf.write(le(width - 1L, 3))
        raf.write(le(height - 1L, 3))
        raf.write(le(durationMs.coerceIn(1, MAX_DURATION).toLong(), 3))
        raf.write(NO_BLEND_KEEP)
        raf.write(data)
        frames++
    }

    fun finish() {
        check(frames > 0) { "No frames" }
        val total = raf.length()
        raf.seek(4L)
        raf.write(le(total - 8L, 4))
    }

    override fun close() {
        raf.close()
    }

    private fun ascii(text: String): ByteArray = text.toByteArray(Charsets.US_ASCII)

    private fun le(value: Long, bytes: Int): ByteArray = ByteArray(bytes) { ((value shr (8 * it)) and 0xFF).toByte() }

    private companion object {
        const val MAX_SIDE = 1 shl 24
        const val MAX_DURATION = (1 shl 24) - 1
        const val FRAME_HEADER = 16L
        const val ANIMATION_FLAG: Byte = 0x02
        const val NO_BLEND_KEEP = 0x02
    }
}
