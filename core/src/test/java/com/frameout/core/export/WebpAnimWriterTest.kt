package com.frameout.core.export

import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WebpAnimWriterTest {
    private fun le(value: Int, bytes: Int) = ByteArray(bytes) { ((value shr (8 * it)) and 0xFF).toByte() }

    private fun simple(payload: ByteArray, fourcc: String = "VP8 "): ByteArray {
        val chunk = ByteArrayOutputStream()
        chunk.write(fourcc.toByteArray())
        chunk.write(le(payload.size, 4))
        chunk.write(payload)
        if (payload.size % 2 == 1) chunk.write(0)
        val body = chunk.toByteArray()
        val out = ByteArrayOutputStream()
        out.write("RIFF".toByteArray())
        out.write(le(body.size + 4, 4))
        out.write("WEBP".toByteArray())
        out.write(body)
        return out.toByteArray()
    }

    private fun vp8(width: Int, height: Int, tail: Int = 0): ByteArray =
        byteArrayOf(0, 0, 0, 0x9D.toByte(), 0x01, 0x2A) + le(width, 2) + le(height, 2) + ByteArray(tail)

    private fun vp8l(width: Int, height: Int): ByteArray =
        byteArrayOf(0x2F) + le((width - 1) or ((height - 1) shl 14), 4)

    private fun read(bytes: ByteArray, at: Int, count: Int): Int = (0 until count).sumOf { (bytes[at + it].toInt() and 0xFF) shl (8 * it) }

    private fun write(frames: List<ByteArray>, durations: List<Int>): ByteArray {
        val file = File.createTempFile("webp_anim", ".webp")
        try {
            WebpAnimWriter(file, 64, 32).use { writer ->
                writer.begin()
                frames.forEachIndexed { i, frame -> writer.addFrame(frame, durations[i]) }
                writer.finish()
            }
            return file.readBytes()
        } finally {
            file.delete()
        }
    }

    @Test
    fun extractKeepsImageChunkAndPadding() {
        val data = WebpFrameChunks.extract(simple(byteArrayOf(1, 2, 3)))
        assertEquals("VP8 ", String(data, 0, 4))
        assertEquals(3, read(data, 4, 4))
        assertEquals(12, data.size)
        assertArrayEquals(byteArrayOf(1, 2, 3, 0), data.copyOfRange(8, 12))
    }

    @Test
    fun extractSkipsMetadataChunks() {
        val inner = simple(byteArrayOf(9, 9)).copyOfRange(12, 22)
        val body = ByteArrayOutputStream().apply {
            write("EXIF".toByteArray())
            write(le(2, 4))
            write(byteArrayOf(7, 7))
            write(inner)
        }.toByteArray()
        val file = ByteArrayOutputStream().apply {
            write("RIFF".toByteArray())
            write(le(body.size + 4, 4))
            write("WEBP".toByteArray())
            write(body)
        }.toByteArray()
        assertEquals(10, WebpFrameChunks.extract(file).size)
    }

    @Test
    fun extractRejectsNonWebp() {
        assertThrows(IllegalArgumentException::class.java) { WebpFrameChunks.extract(ByteArray(40)) }
    }

    @Test
    fun writerProducesAnimatedContainer() {
        val out = write(listOf(simple(vp8(64, 32, 2)), simple(vp8(64, 32, 4))), listOf(80, 90))
        assertEquals("RIFF", String(out, 0, 4))
        assertEquals(out.size - 8, read(out, 4, 4))
        assertEquals("WEBP", String(out, 8, 4))
        assertEquals("VP8X", String(out, 12, 4))
        assertEquals(0x02, out[20].toInt())
        assertEquals(63, read(out, 24, 3))
        assertEquals(31, read(out, 27, 3))
        assertEquals("ANIM", String(out, 30, 4))
        assertEquals("ANMF", String(out, 44, 4))
    }

    @Test
    fun framesCarryDurationAndSize() {
        val out = write(listOf(simple(vp8(64, 32, 2)), simple(vp8(64, 32, 4))), listOf(80, 90))
        val first = 44
        val frameSize = read(out, first + 4, 4)
        assertEquals(63, read(out, first + 14, 3))
        assertEquals(31, read(out, first + 17, 3))
        assertEquals(80, read(out, first + 20, 3))
        val second = first + 8 + frameSize
        assertEquals("ANMF", String(out, second, 4))
        assertEquals(90, read(out, second + 20, 3))
        assertEquals(out.size, second + 8 + read(out, second + 4, 4))
    }

    @Test
    fun finishWithoutFramesFails() {
        val file = File.createTempFile("webp_anim", ".webp")
        try {
            WebpAnimWriter(file, 10, 10).use { writer ->
                writer.begin()
                assertThrows(IllegalStateException::class.java) { writer.finish() }
            }
            assertTrue(file.length() > 0)
        } finally {
            file.delete()
        }
    }

    @Test
    fun dimensionsReadLossyAndLossless() {
        assertEquals(64 to 32, WebpFrameChunks.dimensions(simple(vp8(64, 32))))
        assertEquals(40 to 20, WebpFrameChunks.dimensions(simple(vp8l(40, 20), "VP8L")))
    }

    @Test
    fun addFrameRejectsMismatchedSize() {
        val file = File.createTempFile("webp_anim", ".webp")
        try {
            WebpAnimWriter(file, 64, 32).use { writer ->
                writer.begin()
                assertThrows(IllegalArgumentException::class.java) { writer.addFrame(simple(vp8(32, 32)), 80) }
                assertEquals(0, writer.frameCount)
            }
        } finally {
            file.delete()
        }
    }
}
