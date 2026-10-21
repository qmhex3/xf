package com.frameout.core.storage

import com.frameout.core.hdr.HdrSignal
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

object Png16Writer {
    private val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private const val IDAT_BLOCK = 1 shl 16
    private const val BYTES_PER_PIXEL = 6

    fun write(signal: HdrSignal, extraChunks: ByteArray?, out: OutputStream) {
        out.write(signature)
        out.write(chunk("IHDR", header(signal)))
        out.write(chunk("cICP", byteArrayOf(signal.primaries.toByte(), signal.transfer.toByte(), 0, 1)))
        if (extraChunks != null) out.write(extraChunks)
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION)
        try {
            DeflaterOutputStream(IdatSink(out), deflater, IDAT_BLOCK).use { writeRows(signal, it) }
        } finally {
            deflater.end()
        }
        out.write(chunk("IEND", ByteArray(0)))
        out.flush()
    }

    private fun header(signal: HdrSignal): ByteArray =
        ByteBuffer.allocate(13).putInt(signal.width).putInt(signal.height).put(16).put(2).put(0).put(0).put(0).array()

    private fun writeRows(signal: HdrSignal, sink: OutputStream) {
        val stride = signal.width * BYTES_PER_PIXEL
        val raw = ByteArray(stride)
        val row = ByteArray(stride + 1)
        row[0] = 1
        for (y in 0 until signal.height) {
            fill(signal, y, raw)
            for (i in 0 until stride) {
                val left = if (i >= BYTES_PER_PIXEL) raw[i - BYTES_PER_PIXEL].toInt() else 0
                row[i + 1] = (raw[i].toInt() - left).toByte()
            }
            sink.write(row)
        }
    }

    private fun fill(signal: HdrSignal, y: Int, raw: ByteArray) {
        var from = y * signal.width * 3
        var to = 0
        for (i in 0 until signal.width * 3) {
            val v = signal.rgb[from++].toInt()
            raw[to++] = (v ushr 8).toByte()
            raw[to++] = v.toByte()
        }
    }

    private fun chunk(type: String, data: ByteArray): ByteArray {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        val crc = CRC32().apply { update(typeBytes); update(data) }.value
        return ByteBuffer.allocate(12 + data.size).putInt(data.size).put(typeBytes).put(data).putInt(crc.toInt()).array()
    }

    private class IdatSink(private val out: OutputStream) : OutputStream() {
        private val buffer = ByteArray(IDAT_BLOCK)
        private var length = 0

        override fun write(b: Int) {
            if (length == buffer.size) emit()
            buffer[length++] = b.toByte()
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            var at = off
            var left = len
            while (left > 0) {
                if (length == buffer.size) emit()
                val n = minOf(left, buffer.size - length)
                System.arraycopy(b, at, buffer, length, n)
                length += n
                at += n
                left -= n
            }
        }

        override fun close() {
            if (length > 0) emit()
        }

        private fun emit() {
            out.write(chunk("IDAT", buffer.copyOf(length)))
            length = 0
        }
    }
}
