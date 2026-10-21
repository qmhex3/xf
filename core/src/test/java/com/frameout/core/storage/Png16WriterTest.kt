package com.frameout.core.storage

import com.frameout.core.hdr.HdrSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random
import javax.imageio.ImageIO

class Png16WriterTest {
    private fun signal(w: Int, h: Int, seed: Long = 7): HdrSignal {
        val random = Random(seed)
        val rgb = ShortArray(w * h * 3) { random.nextInt(65536).toShort() }
        for (i in 0 until minOf(rgb.size, 30)) rgb[i] = (i * 2185).toShort()
        return HdrSignal(w, h, rgb, HdrSignal.PRIMARIES_BT2020, HdrSignal.TRANSFER_PQ)
    }

    private fun encode(signal: HdrSignal, extra: ByteArray? = null): ByteArray = ByteArrayOutputStream().also { Png16Writer.write(signal, extra, it) }.toByteArray()

    @Test
    fun roundTripsEverySampleExactly() {
        val s = signal(257, 131)
        val image = ImageIO.read(ByteArrayInputStream(encode(s)))
        assertEquals(257, image.width)
        assertEquals(131, image.height)
        val raster = image.raster
        assertEquals(16, image.colorModel.getComponentSize(0))
        for (y in 0 until s.height) for (x in 0 until s.width) for (c in 0 until 3) {
            assertEquals(s.rgb[(y * s.width + x) * 3 + c].toInt() and 0xFFFF, raster.getSample(x, y, c))
        }
    }

    @Test
    fun writesCicpBeforeImageData() {
        val bytes = encode(signal(8, 4))
        val text = String(bytes, Charsets.ISO_8859_1)
        val cicp = text.indexOf("cICP")
        assertTrue(cicp in 1 until text.indexOf("IDAT"))
        assertEquals(9, bytes[cicp + 4].toInt())
        assertEquals(16, bytes[cicp + 5].toInt())
        assertEquals(0, bytes[cicp + 6].toInt())
        assertEquals(1, bytes[cicp + 7].toInt())
    }

    @Test
    fun largeImageSplitsIntoValidChunks() {
        val s = signal(900, 400, 3)
        val image = ImageIO.read(ByteArrayInputStream(encode(s)))
        assertEquals(s.rgb[1234].toInt() and 0xFFFF, image.raster.getSample(1234 / 3 % 900, 1234 / 3 / 900, 1234 % 3))
    }

    @Test
    fun extraChunksAreKept() {
        val extra = byteArrayOf(0, 0, 0, 0, 't'.code.toByte(), 'E'.code.toByte(), 'X'.code.toByte(), 't'.code.toByte(), 0x1d, 0x3e, 0x8f.toByte(), 0x0a)
        assertTrue(String(encode(signal(4, 4), extra), Charsets.ISO_8859_1).contains("tEXt"))
    }
}
