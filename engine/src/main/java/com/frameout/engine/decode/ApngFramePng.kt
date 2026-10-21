package com.frameout.engine.decode

import com.frameout.codecpack.ApngFrame
import com.frameout.codecpack.ApngIndex
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.CRC32

fun interface ApngRangeReader {
    fun read(offset: Long, length: Int): ByteArray
}

object ApngFramePng {
    private val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    fun build(index: ApngIndex, frame: ApngFrame, reader: ApngRangeReader): ByteArray {
        val total = frame.data.sumOf { it.length.toLong() }
        val out = ByteArrayOutputStream((total + 256L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        val d = DataOutputStream(out)
        d.write(signature)
        writeChunk(d, "IHDR", headerFor(index.ihdr, frame))
        index.shared.forEach { writeChunk(d, it.type, it.data) }
        frame.data.forEach { writeChunk(d, "IDAT", reader.read(it.offset, it.length)) }
        writeChunk(d, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun headerFor(ihdr: ByteArray, frame: ApngFrame): ByteArray {
        val copy = ihdr.copyOf()
        putInt(copy, 0, frame.width)
        putInt(copy, 4, frame.height)
        return copy
    }

    private fun putInt(b: ByteArray, at: Int, value: Int) {
        b[at] = (value ushr 24).toByte()
        b[at + 1] = (value ushr 16).toByte()
        b[at + 2] = (value ushr 8).toByte()
        b[at + 3] = value.toByte()
    }

    private fun writeChunk(d: DataOutputStream, type: String, data: ByteArray) {
        val typeBytes = type.toByteArray(Charsets.ISO_8859_1)
        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data)
        d.writeInt(data.size)
        d.write(typeBytes)
        d.write(data)
        d.writeInt(crc.value.toInt())
    }
}
