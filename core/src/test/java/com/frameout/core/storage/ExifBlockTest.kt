package com.frameout.core.storage

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExifBlockTest {
    private val header = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0, 0)
    private val tiff = byteArrayOf(0x49, 0x49, 0x2A, 0, 8, 0, 0, 0)

    private fun segment(marker: Int, payload: ByteArray): ByteArray {
        val length = payload.size + 2
        return byteArrayOf(0xFF.toByte(), marker.toByte(), (length shr 8).toByte(), length.toByte()) + payload
    }

    private fun jpeg(vararg segments: ByteArray): ByteArray =
        byteArrayOf(0xFF.toByte(), 0xD8.toByte()) + segments.fold(ByteArray(0)) { acc, s -> acc + s } + byteArrayOf(0xFF.toByte(), 0xD9.toByte())

    @Test
    fun extractsExifPayloadStartingAtHeader() {
        val result = ExifBlock.extract(jpeg(segment(0xE1, header + tiff)))
        assertNotNull(result)
        assertArrayEquals(header + tiff, result)
    }

    @Test
    fun skipsRestartMarkersBeforeExif() {
        val rst0 = byteArrayOf(0xFF.toByte(), 0xD0.toByte())
        val result = ExifBlock.extract(jpeg(rst0, segment(0xE1, header + tiff)))
        assertNotNull(result)
    }

    @Test
    fun skipsOtherSegmentsBeforeExif() {
        val app0 = segment(0xE0, byteArrayOf(0x4A, 0x46, 0x49, 0x46, 0, 1, 1))
        val result = ExifBlock.extract(jpeg(app0, segment(0xE1, header + tiff)))
        assertEquals(header.size + tiff.size, result?.size)
    }

    @Test
    fun ignoresNonExifApp1() {
        val xmp = segment(0xE1, "http://ns.adobe.com/xap/1.0/".toByteArray() + byteArrayOf(0))
        assertNull(ExifBlock.extract(jpeg(xmp)))
    }

    @Test
    fun stopsAtStartOfScan() {
        val scan = segment(0xDA, byteArrayOf(1, 2, 3))
        assertNull(ExifBlock.extract(jpeg(scan, segment(0xE1, header + tiff))))
    }

    @Test
    fun rejectsTruncatedOrEmptyInput() {
        assertNull(ExifBlock.extract(ByteArray(0)))
        assertNull(ExifBlock.extract(byteArrayOf(0xFF.toByte(), 0xD8.toByte())))
        val truncated = segment(0xE1, header + tiff).dropLast(4).toByteArray()
        assertNull(ExifBlock.extract(byteArrayOf(0xFF.toByte(), 0xD8.toByte()) + truncated))
    }
}
