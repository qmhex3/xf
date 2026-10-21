package com.frameout.engine

import com.frameout.engine.decode.Av1Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Av1ColorTest {
    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private fun check(av1c: String, full: Boolean, primaries: Int, transfer: Int, matrix: Int) {
        val signal = Av1Color.parse(hex(av1c))
        assertNotNull(signal)
        assertEquals(full, signal!!.fullRange)
        assertTrue(signal.hasDescription)
        assertEquals(primaries, signal.primaries)
        assertEquals(transfer, signal.transfer)
        assertEquals(matrix, signal.matrix)
    }
    @Test
    fun limited709() = check("81000c000a0e000000043cffbcdaf99010101040", false, 1, 1, 1)
    @Test
    fun full601() = check("81000c000a0e000000043cffbcdaf99060606840", true, 6, 6, 6)
    @Test
    fun hdr10() = check("81004c000a0e000000043cffbcdaf9d091009040", false, 9, 16, 9)
    @Test
    fun srgbIdentity() = check("812000000a0d20000002afff9b5f3404340080", true, 1, 13, 0)
    @Test
    fun twelveBitP3Pq() = check("81406c000a0d40000002afff9b5f3d0c100961", false, 12, 16, 9)
    @Test
    fun full444Hlg() = check("812000000a0d20000002afff9b5f3424482640", true, 9, 18, 9)
    @Test
    fun monoFull() = check("81001c000a0d00000002afff9b5f3602020340", true, 1, 1, 1)
    @Test
    fun timingInfo() = check("81000c000a20040000000400000029780000000a530000035f915f90baafff9b5f3202020308", true, 1, 1, 1)
    @Test
    fun svtAv1Bt470bg() = check("81000c000a0d00000002afff805f020a0a0b08", true, 5, 5, 5)
    @Test
    fun bareObusWithoutAv1cHeader() = check("12000a0d00000002afff9b5f3202020f0832c80514002c400619420902b808dd350069e39c6b7d20", true, 1, 1, 7)
    @Test
    fun untaggedLimited() {
        val signal = Av1Color.parse(hex("81000c000a0b000000043cffbcdaf98040"))
        assertNotNull(signal)
        assertFalse(signal!!.hasDescription)
        assertEquals(false, signal.fullRange)
    }
    @Test
    fun untaggedMonoIsFull() {
        val signal = Av1Color.parse(hex("81001c000a0a00000002afff9b5f3540"))
        assertNotNull(signal)
        assertFalse(signal!!.hasDescription)
        assertEquals(true, signal.fullRange)
    }
    @Test
    fun garbageGivesNull() {
        assertNull(Av1Color.parse(ByteArray(0)))
        assertNull(Av1Color.parse(hex("81000c00")))
        assertNull(Av1Color.parse(hex("0a0b0000")))
    }
}
