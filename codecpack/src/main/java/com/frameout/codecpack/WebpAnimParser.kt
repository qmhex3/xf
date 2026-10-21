package com.frameout.codecpack

import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream

class WebpFrameInfo(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val delayMs: Long,
    val disposeToBackground: Boolean,
    val blend: Boolean,
    val dataOffset: Long,
    val dataLength: Int
)

class WebpIndex(val width: Int, val height: Int, val frames: List<WebpFrameInfo>) {
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
}

object WebpAnimParser {
    private const val DEFAULT_DELAY_MS = 100L
    private const val MIN_RELIABLE_DELAY_MS = 10L

    fun normalizedDelay(ms: Long): Long = if (ms <= MIN_RELIABLE_DELAY_MS) DEFAULT_DELAY_MS else ms

    fun parse(input: InputStream): WebpIndex {
        val d = DataInputStream(input)
        val riff = ByteArray(12)
        d.readFully(riff)
        if (!tag(riff, 0, "RIFF") || !tag(riff, 8, "WEBP")) error("Not a WebP file")
        var position = 12L
        var width = 0
        var height = 0
        var animated = false
        val frames = ArrayList<WebpFrameInfo>()
        val header = ByteArray(8)
        while (readHeader(d, header)) {
            val type = String(header, 0, 4, Charsets.ISO_8859_1)
            val size = le32(header, 4)
            if (size < 0) error("Invalid WebP chunk size")
            val padded = size.toLong() + (size and 1)
            val dataAt = position + 8L
            when (type) {
                "VP8X" -> {
                    if (size < 10) error("Invalid VP8X")
                    val p = ByteArray(10).also { d.readFully(it) }
                    animated = (p[0].toInt() and 0x02) != 0
                    width = le24(p, 4) + 1
                    height = le24(p, 7) + 1
                    skip(d, padded - 10L)
                }
                "ANMF" -> {
                    if (size < 16) error("Invalid ANMF")
                    val p = ByteArray(16).also { d.readFully(it) }
                    frames += frameOf(p, dataAt + 16L, size - 16, width, height)
                    skip(d, padded - 16L)
                }
                else -> skip(d, padded)
            }
            position = dataAt + padded
        }
        if (!animated) error("WebP is not animated")
        if (frames.isEmpty()) error("WebP has no frames")
        return WebpIndex(width, height, frames)
    }

    fun isAnimatedHeader(head: ByteArray, length: Int): Boolean {
        if (length < 21 || head.size < 21) return false
        return tag(head, 0, "RIFF") && tag(head, 8, "WEBP") && tag(head, 12, "VP8X") && (head[20].toInt() and 0x02) != 0
    }

    private fun frameOf(p: ByteArray, dataOffset: Long, dataLength: Int, canvasW: Int, canvasH: Int): WebpFrameInfo {
        val x = le24(p, 0) * 2
        val y = le24(p, 3) * 2
        val w = le24(p, 6) + 1
        val h = le24(p, 9) + 1
        val delay = le24(p, 12).toLong()
        val flags = p[15].toInt()
        if (x.toLong() + w > canvasW || y.toLong() + h > canvasH) error("Invalid WebP frame region")
        return WebpFrameInfo(x, y, w, h, normalizedDelay(delay), (flags and 0x01) != 0, (flags and 0x02) == 0, dataOffset, dataLength)
    }

    private fun readHeader(d: DataInputStream, into: ByteArray): Boolean {
        var read = 0
        while (read < into.size) {
            val n = d.read(into, read, into.size - read)
            if (n < 0) return false
            read += n
        }
        return true
    }

    private fun tag(b: ByteArray, at: Int, text: String): Boolean = text.indices.all { b[at + it] == text[it].code.toByte() }

    private fun le24(b: ByteArray, at: Int): Int =
        (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8) or ((b[at + 2].toInt() and 0xFF) shl 16)

    private fun le32(b: ByteArray, at: Int): Int = le24(b, at) or ((b[at + 3].toInt() and 0xFF) shl 24)

    private fun skip(d: DataInputStream, count: Long) {
        var left = count
        while (left > 0) {
            val skipped = d.skip(left)
            if (skipped > 0) left -= skipped else if (d.read() < 0) throw EOFException() else left--
        }
    }
}
