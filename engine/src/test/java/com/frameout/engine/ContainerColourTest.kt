package com.frameout.engine

import com.frameout.engine.decode.MkvColour
import com.frameout.engine.decode.Vp9Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerColourTest {
    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    @Test
    fun vpccLimitedBt709() {
        val colr = Vp9Color.parseVpcc(hex("010000000014820101010000"))
        assertNotNull(colr)
        assertFalse(colr!!.full)
        assertTrue(colr.rangeKnown)
        assertEquals(1, colr.primaries)
        assertEquals(1, colr.transfer)
        assertEquals(1, colr.matrix)
    }

    @Test
    fun vpccUntaggedDoesNotDeclareRange() {
        val colr = Vp9Color.parseVpcc(hex("010000000014820202020000"))
        assertNotNull(colr)
        assertFalse(colr!!.rangeKnown)
    }

    @Test
    fun vpccTooShortOrOldVersionIsIgnored() {
        assertNull(Vp9Color.parseVpcc(hex("0100000000148201")))
        assertNull(Vp9Color.parseVpcc(hex("000000000014820101010000")))
    }

    @Test
    fun matroskaFullRangeBt601() {
        val colours = MkvColour.parse(hex("1a45dfa39f4286810142f7810142f2810442f381084282847765626d4287810242858102185380670100000000004550114d9b74ba4dbb8b53ab841549a96653ac81a14dbb8b53ab841654ae6b53ac81d84dbb8c53ab841254c36753ac8201324dbb8c53ab841c53bb6b53ac82453aec010000000000005900000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001549a966b22ad7b1830f42404d808d4c61766636302e31362e31303057418d4c61766636302e31362e313030448988408f4000000000001654ae6bd5ae010000000000004cd7810173c588031ee77641cc9b769c810022b59c83756e648881008685565f56503983810123e3838405f5e100e09db0820140ba81f09a810255b09055ba810655b1810655bb810655b98102"))
        assertEquals(1, colours.size)
        val colr = colours[0]!!
        assertTrue(colr.full)
        assertTrue(colr.rangeKnown)
        assertEquals(6, colr.primaries)
        assertEquals(6, colr.transfer)
        assertEquals(6, colr.matrix)
    }

    @Test
    fun matroskaHlgLimitedBt2020() {
        val colours = MkvColour.parse(hex("1a45dfa39f4286810142f7810142f2810442f381084282847765626d42878102428581021853806701000000000045eb114d9b74ba4dbb8b53ab841549a96653ac81a14dbb8b53ab841654ae6b53ac81d84dbb8c53ab841254c36753ac8201324dbb8c53ab841c53bb6b53ac8245d5ec010000000000005900000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001549a966b22ad7b1830f42404d808d4c61766636302e31362e31303057418d4c61766636302e31362e313030448988408f4000000000001654ae6bd5ae010000000000004cd7810173c5880953ef1fac53bda69c810022b59c83756e648881008685565f56503983810123e3838405f5e100e09db0820140ba81f09a810255b09055ba811255b1810955bb810955b98101"))
        val colr = colours[0]!!
        assertFalse(colr.full)
        assertTrue(colr.rangeKnown)
        assertEquals(9, colr.primaries)
        assertEquals(18, colr.transfer)
        assertEquals(9, colr.matrix)
    }

    @Test
    fun matroskaGarbageHasNoTracks() {
        assertTrue(MkvColour.parse(ByteArray(0)).isEmpty())
        assertTrue(MkvColour.parse(hex("1a45dfa39f4286")).isEmpty())
    }
}
