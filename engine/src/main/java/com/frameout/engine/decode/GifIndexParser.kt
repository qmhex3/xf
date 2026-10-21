package com.frameout.engine.decode

import java.io.InputStream

internal class GifIndexParser(private val input: InputStream) {
    private class Truncated : IllegalStateException("Unexpected GIF EOF")

    private class Control(val dispose: Int = 0, val transparent: Int = -1)

    private var position = 0L

    fun parse(): GifIndex {
        val header = readBytes(6)
        if (header[0] != 'G'.code.toByte() || header[1] != 'I'.code.toByte() || header[2] != 'F'.code.toByte()) error("Invalid GIF")
        val width = readU16()
        val height = readU16()
        if (width <= 0 || height <= 0) error("Invalid GIF dimensions")
        val packed = readU8()
        readBytes(2)
        val global = if (packed and 0x80 != 0) readBytes(paletteBytes(packed)) else null
        return GifIndex(width, height, global, readFrames())
    }

    private fun readFrames(): List<GifFrameInfo> {
        val frames = ArrayList<GifFrameInfo>()
        var control = Control()
        try {
            while (true) {
                when (readU8()) {
                    0x21 -> control = readExtension() ?: control
                    0x2C -> {
                        frames += readFrame(control)
                        control = Control()
                    }
                    0x3B -> {
                        if (frames.isEmpty()) error("GIF has no frames")
                        return frames
                    }
                    else -> error("Invalid GIF stream")
                }
            }
        } catch (e: Truncated) {
            if (frames.isEmpty()) throw e
            return frames
        }
    }

    private fun readExtension(): Control? {
        val label = readU8()
        if (label != 0xF9) {
            skipSubBlocks()
            return null
        }
        val size = readU8()
        if (size != 4) {
            readBytes(size)
            skipSubBlocks()
            return Control()
        }
        val packed = readU8()
        readU16()
        val transparentIndex = readU8()
        readU8()
        return Control((packed shr 2) and 7, if (packed and 1 != 0) transparentIndex else -1)
    }

    private fun readFrame(control: Control): GifFrameInfo {
        val x = readU16()
        val y = readU16()
        val width = readU16()
        val height = readU16()
        val packed = readU8()
        val palette = if (packed and 0x80 != 0) readBytes(paletteBytes(packed)) else null
        val start = position
        readU8()
        skipSubBlocks()
        return GifFrameInfo(x, y, width, height, packed and 0x40 != 0, palette, control.transparent, control.dispose, start, (position - start).toInt())
    }

    private fun paletteBytes(packed: Int): Int = 3 * (2 shl (packed and 7))

    private fun skipSubBlocks() {
        while (true) {
            val size = readU8()
            if (size == 0) return
            readBytes(size)
        }
    }

    private fun readU8(): Int {
        val value = input.read()
        if (value < 0) throw Truncated()
        position++
        return value
    }

    private fun readU16(): Int = readU8() or (readU8() shl 8)

    private fun readBytes(count: Int): ByteArray {
        val out = ByteArray(count)
        var done = 0
        while (done < count) {
            val read = input.read(out, done, count - done)
            if (read < 0) throw Truncated()
            done += read
        }
        position += count
        return out
    }
}
