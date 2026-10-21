package com.frameout.engine

import com.frameout.engine.decode.VuiColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VuiColorTest {
    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private fun check(sps: String, hevc: Boolean, full: Boolean, primaries: Int, transfer: Int, matrix: Int) {
        val signal = VuiColor.parse(hex(sps), hevc)
        assertNotNull(signal)
        assertEquals(full, signal!!.fullRange)
        assertTrue(signal.hasDescription)
        assertEquals(primaries, signal.primaries)
        assertEquals(transfer, signal.transfer)
        assertEquals(matrix, signal.matrix)
    }

    @Test
    fun avcFullRangeBt709() = check("0000000167640016acd940a02ff97016e0202028000003000800000300a078b16cb0", false, true, 1, 1, 1)

    @Test
    fun avcLimitedBt601() = check("0000000167640016acd940a02ff97016a0c0c0c8000003000800000300a078b16cb0", false, false, 6, 6, 6)

    @Test
    fun avcHighProfileScalingListsHdrTags() = check("0000000167640016acd980a02ff97016e1220128000003000800000300a078b16cd0", false, true, 9, 16, 9)

    @Test
    fun avcWithoutSignalTypeHasNoVui() = assertNull(VuiColor.parse(hex("0000000167640016acd940a02ff97011000003000100000300140f162d96"), false))

    @Test
    fun hevcFullRangeBt709() = check("0000000142010101600000030090000003000003003fa00502016965959a4932bc05b80808082000000300200000030141", true, true, 1, 1, 1)

    @Test
    fun hevcHdr10() = check("0000000142010102200000030090000003000003003fa005020169365959a4932bc05a848804820000030002000003001410", true, false, 9, 16, 9)

    @Test
    fun hevcBframesAndReferenceSets() = check("0000000142010101600000030090000003000003003fa00502016965998a924caf016a0a020a080000030008000003005040", true, false, 5, 1, 5)

    @Test
    fun garbageIsIgnored() {
        assertNull(VuiColor.parse(ByteArray(0), false))
        assertNull(VuiColor.parse(hex("00000001670000"), false))
        assertFalse(VuiColor.parse(hex("0000000142"), true) != null)
    }
}
