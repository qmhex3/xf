package com.frameout.engine.decode

import java.io.DataInputStream
import java.io.InputStream

object GifTimeline {
    fun read(input: InputStream): List<Long> {
        val d = DataInputStream(input)
        val header = ByteArray(6)
        d.readFully(header)
        val width = d.readUnsignedByte() or (d.readUnsignedByte() shl 8)
        val height = d.readUnsignedByte() or (d.readUnsignedByte() shl 8)
        if (width <= 0 || height <= 0) error("Invalid GIF dimensions")
        val packed = d.readUnsignedByte()
        d.skipBytes(2)
        if (packed and 0x80 != 0) skipBytes(d, 3 * (2 shl (packed and 7)))
        val starts = mutableListOf<Long>()
        var timeMs = 0L
        var pendingDelay = 100L
        try {
        while (true) {
            when (d.readUnsignedByte()) {
                0x2C -> {
                    skipBytes(d, 8)
                    val imagePacked = d.readUnsignedByte()
                    if (imagePacked and 0x80 != 0) skipBytes(d, 3 * (2 shl (imagePacked and 7)))
                    d.readUnsignedByte()
                    skipSubBlocks(d)
                    starts += timeMs * 1000L
                    timeMs += pendingDelay
                    pendingDelay = 100L
                }
                0x21 -> readExtension(d).let { if (it != null) pendingDelay = it }
                0x3B -> return if (starts.isEmpty()) listOf(0L) else starts
                else -> error("Invalid GIF stream")
            }
        }
        } catch (e: java.io.EOFException) {
            if (starts.isEmpty()) throw e
            return starts
        }
    }

    private fun readExtension(d: DataInputStream): Long? {
        return when (d.readUnsignedByte()) {
            0xF9 -> readGraphicControl(d)
            0x01 -> { val size = d.readUnsignedByte(); skipBytes(d, size); skipSubBlocks(d); null }
            else -> { skipSubBlocks(d); null }
        }
    }

    private fun readGraphicControl(d: DataInputStream): Long {
        val size = d.readUnsignedByte()
        if (size != 4) { skipBytes(d, size); skipSubBlocks(d); return 100L }
        d.readUnsignedByte()
        val delay = d.readUnsignedByte() or (d.readUnsignedByte() shl 8)
        d.readUnsignedByte()
        d.readUnsignedByte()
        return if (delay <= 1) 100L else delay * 10L
    }

    private fun skipSubBlocks(d: DataInputStream) {
        while (true) {
            val size = d.readUnsignedByte()
            if (size == 0) return
            skipBytes(d, size)
        }
    }

    private fun skipBytes(d: DataInputStream, count: Int) {
        var left = count
        while (left > 0) {
            val skipped = d.skipBytes(left)
            if (skipped > 0) left -= skipped else if (d.read() < 0) throw java.io.EOFException("Unexpected GIF EOF") else left--
        }
    }
}
