package com.frameout.engine

import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractModeCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtractModeCodecTest {
    @Test
    fun customFrameIntervalRoundTrips() {
        val mode = ExtractMode.EveryNFrames(17)
        assertEquals(mode, ExtractModeCodec.decode(ExtractModeCodec.encode(mode)))
    }

    @Test
    fun customMillisRoundTrips() {
        val mode = ExtractMode.EveryNMillis(2500L)
        assertEquals(mode, ExtractModeCodec.decode(ExtractModeCodec.encode(mode)))
    }

    @Test
    fun legacyKeysStillDecode() {
        assertEquals(ExtractMode.EveryNFrames(5), ExtractModeCodec.decode("every_5"))
        assertEquals(ExtractMode.FixedCount(30), ExtractModeCodec.decode("fixed_30"))
    }

    @Test
    fun invalidValuesClampToOne() {
        assertEquals(ExtractMode.EveryNFrames(1), ExtractModeCodec.decode("EveryNFrames:0"))
    }
}
