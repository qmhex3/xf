package com.frameout.engine

import com.frameout.engine.decode.GifTimeline
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class GifTimelineTest {
    @Test
    fun readsFrameStartsFromGraphicControlDelays() {
        val data = byteArrayOf(
            71,73,70,56,57,97,1,0,1,0,0x80.toByte(),0,0,0,0,0,0xFF.toByte(),0,0,
            0x21,0xF9.toByte(),4,0,10,0,0,0,0x2C,0,0,0,0,1,0,1,0,0,2,2,68,1,0,
            0x21,0xF9.toByte(),4,0,20,0,0,0,0x2C,0,0,0,0,1,0,1,0,0,2,2,68,1,0,
            0x3B
        )
        assertEquals(listOf(0L, 100_000L), GifTimeline.read(ByteArrayInputStream(data)))
    }

    @Test
    fun zeroAndOneCentisecondDelaysUseBrowserDefault() {
        val data = byteArrayOf(
            71,73,70,56,57,97,1,0,1,0,0x80.toByte(),0,0,0,0,0,0xFF.toByte(),0,0,
            0x21,0xF9.toByte(),4,0,0,0,0,0,0x2C,0,0,0,0,1,0,1,0,0,2,2,68,1,0,
            0x21,0xF9.toByte(),4,0,1,0,0,0,0x2C,0,0,0,0,1,0,1,0,0,2,2,68,1,0,
            0x21,0xF9.toByte(),4,0,5,0,0,0,0x2C,0,0,0,0,1,0,1,0,0,2,2,68,1,0,
            0x3B
        )
        assertEquals(listOf(0L, 100_000L, 200_000L), GifTimeline.read(ByteArrayInputStream(data)))
    }
}
