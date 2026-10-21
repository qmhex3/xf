package com.frameout.codecpack

import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream

object ApngParser {
    private val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val sharedTypes = setOf("PLTE", "tRNS", "gAMA", "cHRM", "sRGB", "iCCP", "sBIT", "cICP")
    private const val MAX_SHARED = 4 * 1024 * 1024

    fun isAnimated(input: InputStream): Boolean = try {
        val d = DataInputStream(input)
        if (!readSignature(d)) false else scanForAnimationControl(d)
    } catch (_: EOFException) {
        false
    }

    private fun scanForAnimationControl(d: DataInputStream): Boolean {
        while (true) {
            val length = d.readInt()
            when (readType(d)) {
                "acTL" -> return true
                "IDAT", "IEND" -> return false
            }
            skip(d, length.toLong() + 4L)
        }
    }

    fun parse(input: InputStream): ApngIndex {
        val d = DataInputStream(input)
        if (!readSignature(d)) error("Not a PNG file")
        val state = ParseState()
        var position = 8L
        while (!state.finished) {
            val length = d.readInt()
            if (length < 0) error("Invalid PNG chunk length")
            val type = readType(d)
            readChunk(d, state, type, length, position + 8L)
            skip(d, 4L)
            position += 12L + length
        }
        state.flush()
        if (state.frames.isEmpty()) error("APNG has no frames")
        return ApngIndex(state.width, state.height, state.ihdr, state.shared, state.frames)
    }

    private fun readChunk(d: DataInputStream, s: ParseState, type: String, length: Int, dataOffset: Long) {
        when (type) {
            "IHDR" -> {
                s.ihdr = ByteArray(length).also { d.readFully(it) }
                if (length < 13) error("Invalid IHDR")
                s.width = readInt(s.ihdr, 0)
                s.height = readInt(s.ihdr, 4)
            }
            "fcTL" -> s.begin(readFrameControl(d, length, s.width, s.height))
            "IDAT" -> {
                s.seenImageData = true
                s.pendingRanges?.add(ApngRange(dataOffset, length))
                skip(d, length.toLong())
            }
            "fdAT" -> {
                if (length < 4) error("Invalid fdAT")
                s.pendingRanges?.add(ApngRange(dataOffset + 4L, length - 4))
                skip(d, length.toLong())
            }
            "IEND" -> s.finished = true
            else -> {
                if (!s.seenImageData && type in sharedTypes && length <= MAX_SHARED) {
                    s.shared += ApngChunk(type, ByteArray(length).also { d.readFully(it) })
                } else skip(d, length.toLong())
            }
        }
    }

    private fun readFrameControl(d: DataInputStream, length: Int, canvasW: Int, canvasH: Int): FrameHeader {
        if (length < 26) error("Invalid fcTL")
        d.readInt()
        val w = d.readInt()
        val h = d.readInt()
        val x = d.readInt()
        val y = d.readInt()
        val num = d.readUnsignedShort()
        val den = d.readUnsignedShort()
        val dispose = d.readUnsignedByte()
        val blend = d.readUnsignedByte()
        skip(d, (length - 26).toLong())
        if (w <= 0 || h <= 0 || x < 0 || y < 0 || x.toLong() + w > canvasW || y.toLong() + h > canvasH) error("Invalid APNG frame region")
        val ms = if (den == 0) num * 10L else num * 1000L / den
        return FrameHeader(x, y, w, h, ms.coerceAtLeast(10L), dispose.coerceIn(0, 2), blend.coerceIn(0, 1))
    }

    private fun readSignature(d: DataInputStream): Boolean {
        val head = ByteArray(8)
        d.readFully(head)
        return head.contentEquals(signature)
    }

    private fun readType(d: DataInputStream): String {
        val bytes = ByteArray(4)
        d.readFully(bytes)
        return String(bytes, Charsets.ISO_8859_1)
    }

    private fun readInt(b: ByteArray, at: Int): Int =
        ((b[at].toInt() and 0xFF) shl 24) or ((b[at + 1].toInt() and 0xFF) shl 16) or ((b[at + 2].toInt() and 0xFF) shl 8) or (b[at + 3].toInt() and 0xFF)

    private fun skip(d: DataInputStream, count: Long) {
        var left = count
        while (left > 0) {
            val skipped = d.skip(left)
            if (skipped > 0) left -= skipped else if (d.read() < 0) throw EOFException() else left--
        }
    }

    private class FrameHeader(val x: Int, val y: Int, val w: Int, val h: Int, val delayMs: Long, val dispose: Int, val blend: Int)

    private class ParseState {
        var width = 0
        var height = 0
        var ihdr = ByteArray(0)
        var seenImageData = false
        var finished = false
        val shared = mutableListOf<ApngChunk>()
        val frames = mutableListOf<ApngFrame>()
        var pendingHeader: FrameHeader? = null
        var pendingRanges: MutableList<ApngRange>? = null

        fun begin(header: FrameHeader) {
            flush()
            pendingHeader = header
            pendingRanges = mutableListOf()
        }

        fun flush() {
            val h = pendingHeader ?: return
            val ranges = pendingRanges.orEmpty()
            if (ranges.isEmpty()) error("APNG frame without data")
            frames += ApngFrame(h.x, h.y, h.w, h.h, h.delayMs, h.dispose, h.blend, ranges)
            pendingHeader = null
            pendingRanges = null
        }
    }
}
