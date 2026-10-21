package com.frameout.codecpack

import java.io.ByteArrayInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApngGalleryProbeTest {
    private val f = ApngFixture
    private val data = byteArrayOf(1, 2, 3, 4)

    private fun animated(bytes: ByteArray) = ApngParser.isAnimated(ByteArrayInputStream(bytes))

    @Test
    fun animationControlAfterColorChunksIsStillAnimated() {
        val png = f.assemble(f.ihdr(8, 6), f.chunk("gAMA", ByteArray(4)), f.chunk("sRGB", ByteArray(1)), f.actl(3), f.chunk("IDAT", data))
        assertTrue(animated(png))
    }

    @Test
    fun largeProfileBeforeAnimationControlIsSkippedNotRead() {
        val png = f.assemble(f.ihdr(8, 6), f.chunk("iCCP", ByteArray(300_000)), f.actl(2), f.chunk("IDAT", data))
        assertTrue(animated(png))
    }

    @Test
    fun animationControlAfterImageDataDoesNotCount() {
        val png = f.assemble(f.ihdr(8, 6), f.chunk("IDAT", data), f.actl(2))
        assertFalse(animated(png))
    }

    @Test
    fun plainPngWithMetadataIsNotAnimated() {
        val png = f.assemble(f.ihdr(8, 6), f.chunk("tEXt", "Software\u0000x".toByteArray()), f.chunk("IDAT", data))
        assertFalse(animated(png))
    }

    @Test
    fun jpegAndEmptyAndTruncatedAreNotAnimated() {
        assertFalse(animated(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 0x10, 'J'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte())))
        assertFalse(animated(ByteArray(0)))
        val full = f.assemble(f.ihdr(8, 6), f.actl(2), f.chunk("IDAT", data))
        assertFalse(animated(full.copyOf(20)))
    }

    @Test
    fun chunkClaimingHugeLengthEndsWithoutHanging() {
        val head = f.assemble(f.ihdr(8, 6)).copyOf(8 + 25)
        val bogus = byteArrayOf(0x7F, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 'z'.code.toByte(), 'z'.code.toByte(), 'z'.code.toByte(), 'z'.code.toByte(), 1, 2, 3)
        assertFalse(animated(head + bogus))
    }
}
