package com.frameout.codecpack

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WebpAnimParserTest {
    private fun le(value: Int, bytes: Int): ByteArray = ByteArray(bytes) { (value ushr (8 * it)).toByte() }

    private fun chunk(type: String, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(type.toByteArray(Charsets.ISO_8859_1))
        out.write(le(payload.size, 4))
        out.write(payload)
        if (payload.size % 2 == 1) out.write(0)
        return out.toByteArray()
    }

    private fun vp8x(flags: Int, w: Int, h: Int) = chunk("VP8X", byteArrayOf(flags.toByte(), 0, 0, 0) + le(w - 1, 3) + le(h - 1, 3))

    private fun anmf(x: Int, y: Int, w: Int, h: Int, delay: Int, flags: Int, frameData: ByteArray) =
        chunk("ANMF", le(x / 2, 3) + le(y / 2, 3) + le(w - 1, 3) + le(h - 1, 3) + le(delay, 3) + byteArrayOf(flags.toByte()) + frameData)

    private fun riff(vararg chunks: ByteArray): ByteArray {
        val body = ByteArrayOutputStream()
        body.write("WEBP".toByteArray())
        chunks.forEach { body.write(it) }
        val out = ByteArrayOutputStream()
        out.write("RIFF".toByteArray())
        out.write(le(body.size(), 4))
        out.write(body.toByteArray())
        return out.toByteArray()
    }

    private val payloadA = chunk("VP8L", ByteArray(7) { 1 })
    private val payloadB = chunk("VP8 ", ByteArray(10) { 2 })

    private fun sample(): ByteArray = riff(
        vp8x(0x12, 16, 12), chunk("ANIM", ByteArray(6)),
        anmf(0, 0, 16, 12, 40, 0x00, payloadA),
        anmf(4, 2, 8, 6, 5, 0x03, payloadB)
    )

    @Test
    fun readsCanvasFramesRegionsAndFlags() {
        val index = WebpAnimParser.parse(ByteArrayInputStream(sample()))
        assertEquals(16, index.width)
        assertEquals(12, index.height)
        assertEquals(2, index.frames.size)
        val a = index.frames[0]
        assertTrue(a.blend)
        assertFalse(a.disposeToBackground)
        assertEquals(40L, a.delayMs)
        val b = index.frames[1]
        assertEquals(4, b.x); assertEquals(2, b.y); assertEquals(8, b.width); assertEquals(6, b.height)
        assertFalse(b.blend)
        assertTrue(b.disposeToBackground)
    }

    @Test
    fun shortDelaysBecomeOneHundredMillisLikeBrowsers() {
        val index = WebpAnimParser.parse(ByteArrayInputStream(sample()))
        assertEquals(100L, index.frames[1].delayMs)
        assertEquals(100L, WebpAnimParser.normalizedDelay(0))
        assertEquals(11L, WebpAnimParser.normalizedDelay(11))
        assertArrayEquals(longArrayOf(0L, 40_000L), index.startsUs())
        assertEquals(140_000L, index.durationUs())
    }

    @Test
    fun frameDataOffsetsPointAtTheSubChunks() {
        val bytes = sample()
        val index = WebpAnimParser.parse(ByteArrayInputStream(bytes))
        val first = index.frames[0]
        assertEquals(payloadA.size, first.dataLength)
        assertArrayEquals(payloadA, bytes.copyOfRange(first.dataOffset.toInt(), first.dataOffset.toInt() + first.dataLength))
        val second = index.frames[1]
        assertArrayEquals(payloadB, bytes.copyOfRange(second.dataOffset.toInt(), second.dataOffset.toInt() + second.dataLength))
    }

    @Test
    fun staticWebpAndGarbageAreRejected() {
        val still = riff(vp8x(0x10, 16, 12), payloadA)
        try { WebpAnimParser.parse(ByteArrayInputStream(still)); fail() } catch (_: IllegalStateException) { }
        try { WebpAnimParser.parse(ByteArrayInputStream(ByteArray(40) { 3 })); fail() } catch (_: IllegalStateException) { }
    }

    @Test
    fun frameOutsideCanvasIsRejected() {
        val bad = riff(vp8x(0x02, 16, 12), anmf(10, 0, 16, 12, 40, 0, payloadA))
        try { WebpAnimParser.parse(ByteArrayInputStream(bad)); fail() } catch (_: IllegalStateException) { }
    }

    @Test
    fun headerCheckNeedsAnimationBit() {
        val animated = sample()
        assertTrue(WebpAnimParser.isAnimatedHeader(animated, 32))
        val still = riff(vp8x(0x10, 16, 12), payloadA)
        assertFalse(WebpAnimParser.isAnimatedHeader(still, 32))
        assertFalse(WebpAnimParser.isAnimatedHeader(animated, 10))
    }
}
