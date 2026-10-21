package com.frameout.core.storage

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PngMetadataTest {
    private fun chunk(type: String, data: ByteArray): ByteArray {
        val t = type.toByteArray(Charsets.US_ASCII)
        val crc = CRC32().apply { update(t); update(data) }.value
        return ByteBuffer.allocate(12 + data.size).putInt(data.size).put(t).put(data).putInt(crc.toInt()).array()
    }

    private fun samplePng(): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        out.write(chunk("IHDR", ByteBuffer.allocate(13).putInt(1).putInt(1).put(8).put(2).put(0).put(0).put(0).array()))
        out.write(chunk("IDAT", byteArrayOf(1, 2, 3)))
        out.write(chunk("IEND", ByteArray(0)))
        return out.toByteArray()
    }

    private fun chunks(png: ByteArray): List<Pair<String, ByteArray>> {
        val list = ArrayList<Pair<String, ByteArray>>()
        var pos = 8
        while (pos < png.size) {
            val length = ByteBuffer.wrap(png, pos, 4).int
            val type = String(png, pos + 4, 4, Charsets.US_ASCII)
            val data = png.copyOfRange(pos + 8, pos + 8 + length)
            val crc = CRC32().apply { update(png, pos + 4, 4 + length) }.value.toInt()
            assertEquals(crc, ByteBuffer.wrap(png, pos + 8 + length, 4).int)
            list += type to data
            pos += 12 + length
        }
        return list
    }

    @Test
    fun insertsMetadataChunksRightAfterHeader() {
        val result = PngMetadata.embed(samplePng(), 1709622489007L, "Trip · #1 · 00:01.000", GeoPoint(31.5, 35.0))
        val names = chunks(result).map { it.first }
        assertEquals(listOf("IHDR", "eXIf", "tEXt", "iTXt", "IDAT", "IEND"), names)
    }

    @Test
    fun keepsImageDataUntouched() {
        val original = samplePng()
        val result = PngMetadata.embed(original, 0L, "x", null)
        assertEquals(chunks(original).last().first, chunks(result).last().first)
        assertArrayEquals(chunks(original)[1].second, chunks(result).first { it.first == "IDAT" }.second)
    }

    @Test
    fun ignoresNonPngBytes() {
        val junk = ByteArray(64) { it.toByte() }
        assertFalse(PngMetadata.isPng(junk))
        assertArrayEquals(junk, PngMetadata.embed(junk, 0L, "x", null))
    }

    @Test
    fun replacesPreviousFrameOutMetadataWithoutDuplicatingIt() {
        val once = PngMetadata.embed(samplePng(), 1709622489000L, "old", GeoPoint(31.5, 35.0))
        val twice = PngMetadata.embed(once, 1709622490000L, "new", null)
        val names = chunks(twice).map { it.first }
        assertEquals(listOf("IHDR", "eXIf", "tEXt", "iTXt", "IDAT", "IEND"), names)
        assertEquals(1, names.count { it == "eXIf" })
        assertEquals(1, names.count { it == "tEXt" })
        assertEquals(1, names.count { it == "iTXt" })
        assertFalse(String(twice, Charsets.UTF_8).contains("old"))
    }

    @Test
    fun rejectsBadCrcAndTruncatedPng() {
        val original = samplePng()
        val badCrc = original.clone().also { it[ihdrCrcOffset(original.size)] = (it[ihdrCrcOffset(original.size)].toInt() xor 0x01).toByte() }
        assertFalse(PngMetadata.isPng(badCrc))
        assertFalse(PngMetadata.isPng(original.copyOf(original.size - 1)))
    }

    private fun ihdrCrcOffset(size: Int): Int = 8 + 4 + 4 + 13

    @Test
    fun exifBlockContainsGpsOnlyWhenLocationGiven() {
        val without = PngMetadata.exif(1709622489000L, "d", null)
        val with = PngMetadata.exif(1709622489000L, "d", GeoPoint(31.5, 35.0))
        assertTrue(with.size > without.size)
        assertEquals('M'.code.toByte(), with[0])
        assertEquals(0x2A.toByte(), with[3])
        assertEquals(8, ByteBuffer.wrap(with, 4, 4).int)
    }

    @Test
    fun exifBlockStoresDateText() {
        val block = String(PngMetadata.exif(1709622489000L, "d", null), Charsets.ISO_8859_1)
        assertTrue(block.contains(ExifText.dateTime(1709622489000L)))
    }
}
