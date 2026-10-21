package com.frameout.core.export

import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPhotoWriterTest {
    private val jfif = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 16,
        'J'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 0, 1, 1, 0, 0, 1, 0, 1, 0, 0,
        0xFF.toByte(), 0xDB.toByte(), 0, 4, 1, 2, 0xFF.toByte(), 0xD9.toByte()
    )

    @Test
    fun xmpIsInsertedAfterJfifAndKeepsTheRestIntact() {
        val xmp = MotionPhotoWriter.xmp(10L, 0L)
        val tagged = MotionPhotoWriter.withXmp(jfif, xmp)
        assertArrayEquals(jfif.copyOfRange(0, 20), tagged.copyOfRange(0, 20))
        assertEquals(0xFF.toByte(), tagged[20])
        assertEquals(0xE1.toByte(), tagged[21])
        val length = ((tagged[22].toInt() and 0xFF) shl 8) or (tagged[23].toInt() and 0xFF)
        assertEquals(tagged.size - jfif.size - 2, length)
        assertArrayEquals(jfif.copyOfRange(20, jfif.size), tagged.copyOfRange(tagged.size - (jfif.size - 20), tagged.size))
    }

    @Test
    fun fileEndsWithTheVideoAndOffsetMatchesItsLength() {
        val video = File.createTempFile("clip", ".mp4")
        video.writeBytes(ByteArray(777) { (it % 251).toByte() })
        val out = ByteArrayOutputStream()
        MotionPhotoWriter.write(jfif, video, 1_500_000L, out)
        val bytes = out.toByteArray()
        val text = String(bytes, Charsets.ISO_8859_1)
        assertTrue(text.contains("GCamera:MicroVideoOffset=\"777\""))
        assertTrue(text.contains("GCamera:MotionPhotoPresentationTimestampUs=\"1500000\""))
        assertArrayEquals(video.readBytes(), bytes.copyOfRange(bytes.size - 777, bytes.size))
        video.delete()
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonJpeg() {
        MotionPhotoWriter.withXmp(ByteArray(10), ByteArray(4))
    }
}
