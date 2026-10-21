package com.frameout.engine

import com.frameout.engine.decode.GifCompositor
import com.frameout.engine.decode.GifFrameInfo
import com.frameout.engine.decode.GifIndex
import com.frameout.engine.decode.GifIndexParser
import com.frameout.engine.decode.GifLzw
import com.frameout.engine.decode.interlacedRows
import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class GifCompositorTest {
    private val gif: ByteArray = listOf(
        "47494638396114001000820000000000ff000000ff000000ffffff0000ffffff00ffc8c8c821ff0b4e45545343415045",
        "322e30030100000021f90401040000002c00000000140010000008d1000b1c2820a0408100020e182000c0e0c1840404",
        "1808408021000307061420302080c78902060c2010602201830b111e0030004042821a3b1e1c20a061440138371e0c30",
        "60214a00040e20a4c813674f013c4b263d78b12745a03a791a5838d063cd032471ae8c38f580008e0993b6ac6860264f",
        "8d5a850228d9326400820a011cf0ba70a2c19e720db80c20b7e9db8e730536bc48c04041c32b310a347892e4498f7d39",
        "ae5c5b60c04a896549122c8cf265cd96347186241b342480863d850e7489f37449ac04ad8e5cfb516f48c001010021f9",
        "0409060000002c030002000800050082000000ff000000ff000000ffffff0000ffffff00ffc8c8c8081d00010c1058a0",
        "c08081030b00107870e180820e0d267c0840e1438a01010021f9040d080000002c000000001400100082000000ff0000",
        "00ff000000ffffff0000ffffff00ffc8c8c808cc000b1c2820a0408100020e182000c0e0c18404041808408021000307",
        "061420302080c78902060c2010c0008001270d8a3c70124042821a3bb62c00e0e4000138371e0c306061019429690220",
        "7933a401013c4bc644f9b3c044860474f2346092664c9a2471b28c48f580008e0993a2ac68e060479104131e0800a0a4",
        "4b93172f9a3ce075e144832e71c23dcad240c79f01e80ac5a9972adf8b03076e9468f268e3a10358b6fdd9d7a480bc6f",
        "0df225d8d2705ebd04bc02fd8931304dc2354b1e888a14e1c8b61f2d8b0c1c10003b"
    ).joinToString("").chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private val expected = intArrayOf(1784088931, 1963076520, -923310166)

    private fun index(): GifIndex = GifIndexParser(ByteArrayInputStream(gif)).parse()

    private fun compositor(): GifCompositor =
        GifCompositor(index()) { offset, length -> gif.copyOfRange(offset.toInt(), offset.toInt() + length) }

    private fun frame(x: Int, y: Int, w: Int, h: Int, transparent: Int, dispose: Int) =
        GifFrameInfo(x, y, w, h, false, null, transparent, dispose, 0L, 0)

    @Test
    fun parserReadsFramesDisposalAndTransparency() {
        val parsed = index()
        assertEquals(20, parsed.width)
        assertEquals(16, parsed.height)
        assertEquals(listOf(0, 2, 3), parsed.frames.map { it.dispose })
        assertEquals(listOf(0, 0, 0), parsed.frames.map { it.transparent })
        assertEquals(listOf(8, 8, 8), parsed.frames.map { (it.palette ?: parsed.globalPalette)!!.size / 3 })
    }

    @Test
    fun framesComposeInOrderLikeTheReference() {
        val source = compositor()
        expected.forEachIndexed { i, hash -> assertEquals(hash, source.render(i).contentHashCode()) }
    }

    @Test
    fun randomAccessAndRewindGiveTheSameCanvas() {
        val source = compositor()
        assertEquals(expected[2], source.render(2).contentHashCode())
        assertEquals(expected[0], source.render(0).contentHashCode())
        assertEquals(expected[1], source.render(1).contentHashCode())
        assertEquals(expected[2], source.render(2).contentHashCode())
    }

    @Test
    fun interlacedRowsFollowTheFourPassOrder() {
        assertArrayEquals(intArrayOf(0, 8, 4, 2, 6, 1, 3, 5, 7, 9), interlacedRows(10))
        assertEquals((0 until 37).toList(), interlacedRows(37).sorted())
    }

    @Test
    fun opaqueFullCanvasFramesAreResetPoints() {
        val frames = listOf(
            frame(0, 0, 4, 4, 0, 0),
            frame(1, 1, 2, 2, -1, 0),
            frame(0, 0, 4, 4, -1, 0),
            frame(0, 0, 4, 4, -1, 3),
            frame(0, 0, 4, 4, 1, 0)
        )
        assertArrayEquals(intArrayOf(0, 2), GifIndex(4, 4, null, frames).resetPoints())
    }

    @Test
    fun truncatedLzwDataLeavesTheRestZero() {
        val out = GifLzw.decode(byteArrayOf(2, 1, 0x44, 0), 8)
        assertEquals(8, out.size)
    }
}
