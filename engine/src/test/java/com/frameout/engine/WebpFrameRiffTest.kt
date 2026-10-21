package com.frameout.engine

import com.frameout.engine.decode.ImageHead
import com.frameout.engine.decode.WebpFrameRiff
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebpFrameRiffTest {
    private fun le32(b: ByteArray, at: Int) = (0..3).sumOf { (b[at + it].toInt() and 0xFF) shl (8 * it) }
    private fun le24(b: ByteArray, at: Int) = (0..2).sumOf { (b[at + it].toInt() and 0xFF) shl (8 * it) }
    private fun text(b: ByteArray, at: Int) = String(b, at, 4, Charsets.ISO_8859_1)

    @Test
    fun wrapsFrameDataInAStandaloneWebpWithFrameSizedCanvas() {
        val data = ByteArray(16) { (it + 1).toByte() }
        val webp = WebpFrameRiff.build(40, 30, data)
        assertEquals("RIFF", text(webp, 0))
        assertEquals("WEBP", text(webp, 8))
        assertEquals("VP8X", text(webp, 12))
        assertEquals(10, le32(webp, 16))
        assertEquals(39, le24(webp, 24))
        assertEquals(29, le24(webp, 27))
        assertEquals(webp.size - 8, le32(webp, 4))
        assertArrayEquals(data, webp.copyOfRange(30, 30 + data.size))
    }

    @Test
    fun oddFrameDataIsPaddedSoRiffSizeStaysEven() {
        val webp = WebpFrameRiff.build(2, 2, ByteArray(5) { 9 })
        assertEquals(0, webp.size % 2)
        assertEquals(webp.size - 8, le32(webp, 4))
        assertEquals(0, webp.last().toInt())
    }

    @Test
    fun headClassifierSeparatesAnimatedWebpAvifAndOthers() {
        val webp = ByteArray(32)
        "RIFF".forEachIndexed { i, c -> webp[i] = c.code.toByte() }
        "WEBP".forEachIndexed { i, c -> webp[8 + i] = c.code.toByte() }
        "VP8X".forEachIndexed { i, c -> webp[12 + i] = c.code.toByte() }
        webp[20] = 0x02
        assertEquals(ImageHead.Kind.AnimatedWebp, ImageHead.kindOf(webp, 32))
        webp[20] = 0x10
        assertEquals(ImageHead.Kind.Other, ImageHead.kindOf(webp, 32))

        val avif = ByteArray(32)
        "ftyp".forEachIndexed { i, c -> avif[4 + i] = c.code.toByte() }
        "avis".forEachIndexed { i, c -> avif[8 + i] = c.code.toByte() }
        assertEquals(ImageHead.Kind.Avif, ImageHead.kindOf(avif, 32))
        assertEquals(ImageHead.Kind.Other, ImageHead.kindOf(ByteArray(32) { 1 }, 32))
        assertTrue(ImageHead.kindOf(ByteArray(3), 3) == ImageHead.Kind.Other)
    }
}
