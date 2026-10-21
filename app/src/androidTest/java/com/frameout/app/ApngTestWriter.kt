package com.frameout.app

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.CRC32

class ApngFrameSpec(
    val x: Int,
    val y: Int,
    val dispose: Int,
    val blend: Int,
    val delayNum: Int,
    val delayDen: Int,
    val bitmap: () -> Bitmap
)

object ApngTestWriter {
    private val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    fun write(file: File, width: Int, height: Int, frames: Int, spec: (Int) -> ApngFrameSpec) {
        DataOutputStream(file.outputStream().buffered()).use { out ->
            out.write(signature)
            var seq = 0
            for (i in 0 until frames) {
                val s = spec(i)
                val bitmap = s.bitmap()
                val png = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
                val w = bitmap.width
                val h = bitmap.height
                bitmap.recycle()
                val chunks = split(png)
                if (i == 0) {
                    val header = chunks.first { it.first == "IHDR" }.second.copyOf()
                    putInt(header, 0, width)
                    putInt(header, 4, height)
                    chunk(out, "IHDR", header)
                    chunk(out, "acTL", ints(frames, 0))
                }
                chunk(out, "fcTL", frameControl(seq++, w, h, s))
                chunks.filter { it.first == "IDAT" }.forEach { (_, data) ->
                    if (i == 0) chunk(out, "IDAT", data) else chunk(out, "fdAT", ints(seq++) + data)
                }
            }
            chunk(out, "IEND", ByteArray(0))
        }
    }

    private fun frameControl(seq: Int, w: Int, h: Int, s: ApngFrameSpec): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).apply {
            writeInt(seq); writeInt(w); writeInt(h); writeInt(s.x); writeInt(s.y)
            writeShort(s.delayNum); writeShort(s.delayDen); writeByte(s.dispose); writeByte(s.blend)
        }
        return bytes.toByteArray()
    }

    private fun split(png: ByteArray): List<Pair<String, ByteArray>> {
        val d = DataInputStream(png.inputStream(8, png.size - 8))
        val out = mutableListOf<Pair<String, ByteArray>>()
        while (d.available() > 0) {
            val length = d.readInt()
            val type = ByteArray(4).also { d.readFully(it) }
            val data = ByteArray(length).also { d.readFully(it) }
            d.readInt()
            out += String(type, Charsets.ISO_8859_1) to data
        }
        return out
    }

    private fun ints(vararg values: Int): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).apply { values.forEach { writeInt(it) } }
        return bytes.toByteArray()
    }

    private fun putInt(b: ByteArray, at: Int, value: Int) {
        for (k in 0 until 4) b[at + k] = (value ushr (24 - 8 * k)).toByte()
    }

    private fun chunk(out: DataOutputStream, type: String, data: ByteArray) {
        val typeBytes = type.toByteArray(Charsets.ISO_8859_1)
        val crc = CRC32().apply { update(typeBytes); update(data) }
        out.writeInt(data.size)
        out.write(typeBytes)
        out.write(data)
        out.writeInt(crc.value.toInt())
    }
}
