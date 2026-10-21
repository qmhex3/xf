package com.frameout.core.export

import java.io.ByteArrayOutputStream
import java.util.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class GifLzwEncoderTest {
    private fun encode(pixels: ByteArray): ByteArray = ByteArrayOutputStream().also { GifLzwEncoder(it).encode(pixels) }.toByteArray()

    private fun decode(data: ByteArray): ByteArray {
        val minSize = data[0].toInt()
        val bits = ArrayList<Byte>()
        var at = 1
        while (data[at].toInt() != 0) {
            val length = data[at].toInt() and 0xFF
            for (i in 1..length) bits.add(data[at + i])
            at += length + 1
        }
        val clear = 1 shl minSize
        val end = clear + 1
        val table = ArrayList<ByteArray>()
        fun reset() { table.clear(); for (i in 0 until clear) table.add(byteArrayOf(i.toByte())); table.add(ByteArray(0)); table.add(ByteArray(0)) }
        reset()
        var size = minSize + 1
        var buffer = 0
        var count = 0
        var index = 0
        var previous: ByteArray? = null
        val out = ByteArrayOutputStream()
        while (true) {
            while (count < size && index < bits.size) { buffer = buffer or ((bits[index++].toInt() and 0xFF) shl count); count += 8 }
            val code = buffer and ((1 shl size) - 1)
            buffer = buffer ushr size
            count -= size
            if (code == clear) { reset(); size = minSize + 1; previous = null; continue }
            if (code == end) break
            val entry = if (code < table.size) table[code] else previous!! + previous[0]
            out.write(entry)
            if (previous != null && table.size < 4096) table.add(previous + entry[0])
            if (table.size == (1 shl size) && size < 12) size++
            previous = entry
        }
        return out.toByteArray()
    }

    @Test
    fun roundTripsRepeatingPattern() {
        val pixels = ByteArray(5000) { (it % 7).toByte() }
        assertArrayEquals(pixels, decode(encode(pixels)))
    }

    @Test
    fun roundTripsNoiseBeyondTableLimit() {
        val random = Random(3)
        val pixels = ByteArray(200_000) { random.nextInt(256).toByte() }
        assertArrayEquals(pixels, decode(encode(pixels)))
    }

    @Test
    fun quantizerMapsPrimaryColorsToPaletteEntries() {
        val indices = GifQuantizer.indices(intArrayOf(0xFFFF0000.toInt(), 0xFF000000.toInt()), 2, 1)
        assertEquals(255, GifQuantizer.palette[(indices[0].toInt() and 0xFF) * 3].toInt() and 0xFF)
        assertEquals(0, indices[1].toInt())
    }
}
