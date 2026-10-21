package com.frameout.engine

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.CRC32

object ApngTestBytes {
    private val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    fun chunk(type: String, data: ByteArray): ByteArray {
        val typeBytes = type.toByteArray(Charsets.ISO_8859_1)
        val crc = CRC32().apply { update(typeBytes); update(data) }
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply { writeInt(data.size); write(typeBytes); write(data); writeInt(crc.value.toInt()) }
        return out.toByteArray()
    }

    fun ihdr(width: Int, height: Int): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply { writeInt(width); writeInt(height); write(byteArrayOf(8, 3, 0, 0, 0)) }
        return chunk("IHDR", out.toByteArray())
    }

    fun fctl(seq: Int, w: Int, h: Int, x: Int, y: Int): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeInt(seq); writeInt(w); writeInt(h); writeInt(x); writeInt(y)
            writeShort(1); writeShort(10); writeByte(0); writeByte(0)
        }
        return chunk("fcTL", out.toByteArray())
    }

    fun fdat(seq: Int, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply { writeInt(seq); write(payload) }
        return chunk("fdAT", out.toByteArray())
    }

    fun actl(frames: Int): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply { writeInt(frames); writeInt(0) }
        return chunk("acTL", out.toByteArray())
    }

    fun assemble(vararg parts: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(signature)
        parts.forEach { out.write(it) }
        out.write(chunk("IEND", ByteArray(0)))
        return out.toByteArray()
    }
}
