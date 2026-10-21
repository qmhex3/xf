package com.frameout.engine

import com.frameout.codecpack.ApngParser
import com.frameout.engine.decode.ApngFramePng
import com.frameout.engine.decode.ApngRangeReader
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.zip.CRC32
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApngFramePngTest {
    private val t = ApngTestBytes
    private val first = byteArrayOf(1, 2, 3, 4, 5)
    private val second = byteArrayOf(9, 8, 7)
    private val third = byteArrayOf(6, 6)
    private val palette = ByteArray(6) { (it + 1).toByte() }

    private val source = t.assemble(
        t.ihdr(16, 12), t.chunk("PLTE", palette), t.actl(2),
        t.fctl(0, 16, 12, 0, 0), t.chunk("IDAT", first),
        t.fctl(1, 5, 4, 3, 2), t.fdat(2, second), t.fdat(3, third)
    )

    private class Chunk(val type: String, val data: ByteArray, val crcOk: Boolean)

    private fun readChunks(png: ByteArray): List<Chunk> {
        val d = DataInputStream(ByteArrayInputStream(png, 8, png.size - 8))
        val out = mutableListOf<Chunk>()
        while (d.available() > 0) {
            val length = d.readInt()
            val type = ByteArray(4).also { d.readFully(it) }
            val data = ByteArray(length).also { d.readFully(it) }
            val crc = CRC32().apply { update(type); update(data) }.value.toInt()
            out += Chunk(String(type, Charsets.ISO_8859_1), data, d.readInt() == crc)
        }
        return out
    }

    private fun reader() = ApngRangeReader { offset, length -> source.copyOfRange(offset.toInt(), offset.toInt() + length) }

    private fun build(frame: Int): ByteArray {
        val index = ApngParser.parse(ByteArrayInputStream(source))
        return ApngFramePng.build(index, index.frames[frame], reader())
    }

    @Test
    fun startsWithPngSignatureAndEndsWithIend() {
        val png = build(0)
        assertArrayEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A), png.copyOf(8))
        assertEquals("IEND", readChunks(png).last().type)
    }

    @Test
    fun everyChunkHasValidCrc() {
        assertTrue(readChunks(build(0)).all { it.crcOk })
        assertTrue(readChunks(build(1)).all { it.crcOk })
    }

    @Test
    fun headerCarriesFrameDimensions() {
        val header = readChunks(build(1)).first { it.type == "IHDR" }.data
        val d = DataInputStream(ByteArrayInputStream(header))
        assertEquals(5, d.readInt())
        assertEquals(4, d.readInt())
        assertEquals(8, header[8].toInt())
        assertEquals(3, header[9].toInt())
    }

    @Test
    fun keepsPaletteBetweenHeaderAndImageData() {
        val types = readChunks(build(0)).map { it.type }
        assertEquals(listOf("IHDR", "PLTE", "IDAT", "IEND"), types)
        assertArrayEquals(palette, readChunks(build(0)).first { it.type == "PLTE" }.data)
    }

    @Test
    fun rewritesFdatPayloadsAsImageDataChunks() {
        val chunks = readChunks(build(1)).filter { it.type == "IDAT" }
        assertEquals(2, chunks.size)
        assertArrayEquals(second, chunks[0].data)
        assertArrayEquals(third, chunks[1].data)
    }

    @Test
    fun defaultFrameUsesOriginalImageData() {
        val chunks = readChunks(build(0)).filter { it.type == "IDAT" }
        assertArrayEquals(first, chunks.single().data)
    }
}
