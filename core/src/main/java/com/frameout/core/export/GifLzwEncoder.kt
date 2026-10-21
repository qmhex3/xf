package com.frameout.core.export

import java.io.OutputStream

class GifLzwEncoder(private val out: OutputStream, private val minCodeSize: Int = 8) {
    private val block = ByteArray(255)
    private var blockLength = 0
    private var bitBuffer = 0
    private var bitCount = 0

    fun encode(pixels: ByteArray) {
        val clear = 1 shl minCodeSize
        val end = clear + 1
        val table = HashMap<Int, Int>()
        var next = end + 1
        var size = minCodeSize + 1
        out.write(minCodeSize)
        emit(clear, size)
        var prefix = pixels[0].toInt() and 0xFF
        for (i in 1 until pixels.size) {
            val value = pixels[i].toInt() and 0xFF
            val key = (prefix shl 8) or value
            val known = table[key]
            if (known != null) {
                prefix = known
                continue
            }
            emit(prefix, size)
            if (next >= (1 shl size) && size < 12) size++
            if (next < 4096) {
                table[key] = next++
            } else {
                emit(clear, size)
                table.clear()
                next = end + 1
                size = minCodeSize + 1
            }
            prefix = value
        }
        emit(prefix, size)
        if (next >= (1 shl size) && size < 12) size++
        emit(end, size)
        if (bitCount > 0) put(bitBuffer and 0xFF)
        flushBlock()
        out.write(0)
    }

    private fun emit(code: Int, size: Int) {
        bitBuffer = bitBuffer or (code shl bitCount)
        bitCount += size
        while (bitCount >= 8) {
            put(bitBuffer and 0xFF)
            bitBuffer = bitBuffer ushr 8
            bitCount -= 8
        }
    }

    private fun put(byte: Int) {
        block[blockLength++] = byte.toByte()
        if (blockLength == 255) flushBlock()
    }

    private fun flushBlock() {
        if (blockLength == 0) return
        out.write(blockLength)
        out.write(block, 0, blockLength)
        blockLength = 0
    }
}
