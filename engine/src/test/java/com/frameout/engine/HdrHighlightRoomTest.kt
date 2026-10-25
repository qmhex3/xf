package com.frameout.engine

import android.media.MediaFormat
import com.frameout.engine.decode.ColorParams
import com.frameout.engine.decode.GainCodec
import com.frameout.engine.decode.HdrTables
import com.frameout.engine.decode.HdrToneMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow

class HdrHighlightRoomTest {
    private fun params(peak: Float = 1000f, primaries: Int = -1) =
        ColorParams(true, true, MediaFormat.COLOR_TRANSFER_ST2084, peak, primaries = primaries)

    private fun mapper(peak: Float = 1000f) = HdrToneMapper(params(peak))

    private fun signal(nits: Double): Float {
        val p = (nits / 10000.0).coerceIn(0.0, 1.0).pow(M1)
        return ((C1 + C2 * p) / (1.0 + C3 * p)).pow(M2).toFloat()
    }

    private fun level(mapper: HdrToneMapper, nits: Double): Int {
        val s = signal(nits)
        return (mapper.map(s, s, s) shr 8) and 255
    }

    private fun srgbCode(linear: Double): Int {
        val v = if (linear <= 0.0031308) linear * 12.92 else 1.055 * linear.pow(1.0 / 2.4) - 0.055
        return (v * 255.0 + 0.5).toInt()
    }

    private fun srgbLinear(code: Int): Double {
        val v = code / 255.0
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun pqLinear(v: Float): Double {
        val p = v.toDouble().pow(1.0 / M2)
        return 10000.0 * (maxOf(p - C1, 0.0) / (C2 - C3 * p)).pow(1.0 / M1) / HdrTables.REF_WHITE
    }

    @Test
    fun shadowsAndMidtonesFollowPlainSrgbUpToTheKnee() {
        val m = mapper()
        listOf(1.0, 5.0, 18.0, 50.0, 100.0).forEach {
            assertEquals(srgbCode(it / HdrTables.REF_WHITE).toDouble(), level(m, it).toDouble(), 1.0)
        }
    }

    @Test
    fun toneCurveIsIdentityUpToItsKneeAndCompressesAboveIt() {
        val m = mapper()
        assertEquals(0.3f, m.toneLuma(0.3f), 0f)
        assertEquals(HdrTables.TONE_KNEE, m.toneLuma(HdrTables.TONE_KNEE), 0f)
        assertTrue(m.toneLuma(0.6f) < 0.6f)
        val step = 1e-3f
        val slope = (m.toneLuma(HdrTables.TONE_KNEE + step) - m.toneLuma(HdrTables.TONE_KNEE)) / step
        assertEquals(1f, slope, 0.02f)
    }

    @Test
    fun desaturationKneeStaysIndependentOfTheToneKnee() {
        val m = mapper()
        assertTrue(HdrTables.TONE_KNEE < HdrTables.DESAT_KNEE)
        assertEquals(0f, m.desaturation(HdrTables.DESAT_KNEE), 0f)
        assertEquals(0f, m.desaturation(0.6f), 0f)
        assertTrue(m.desaturation(1.5f) > 0f)
    }

    @Test
    fun diffuseWhiteKeepsMostOfItsBrightness() {
        val code = level(mapper(), 203.0)
        assertTrue(code in 226..231)
    }

    @Test
    fun everyStopAboveDiffuseWhiteGetsRoom() {
        val m = mapper()
        val white = level(m, 203.0)
        val double = level(m, 406.0)
        val quad = level(m, 812.0)
        assertTrue(double - white >= 14)
        assertTrue(quad - double >= 7)
    }

    @Test
    fun highlightsAreNotClippedBeforeThePeak() {
        val m = mapper()
        assertTrue(level(m, 500.0) <= 249)
        assertTrue(level(m, 800.0) < 255)
        assertEquals(255, level(m, 1000.0))
    }

    @Test
    fun gradientAboveThreeHundredNitsKeepsManyDistinctLevels() {
        val m = mapper()
        val seen = HashSet<Int>()
        var last = -1
        for (nits in 300..1000) {
            val code = level(m, nits.toDouble())
            assertTrue(code >= last)
            last = code
            seen.add(code)
        }
        assertTrue(seen.size >= 14)
    }

    @Test
    fun lowerPeakContentStillReachesWhiteAtItsOwnPeak() {
        val m = mapper(600f)
        assertEquals(255, level(m, 600.0))
        assertTrue(level(m, 300.0) < 255)
        assertEquals(1f, m.toneLuma(600f / HdrTables.REF_WHITE), 0.001f)
    }

    @Test
    fun grayStaysNeutralAcrossTheHighlightRange() {
        val m = mapper()
        for (nits in 200..1000 step 25) {
            val s = signal(nits.toDouble())
            val p = m.map(s, s, s)
            val r = (p shr 16) and 255
            val g = (p shr 8) and 255
            val b = p and 255
            assertTrue(abs(r - g) <= 1)
            assertTrue(abs(g - b) <= 1)
        }
    }

    @Test
    fun gainMapStillRebuildsHdrInTheHighlights() {
        val m = HdrToneMapper(params(primaries = 0), trackGain = true)
        val samples = listOf(
            floatArrayOf(0.62f, 0.62f, 0.62f),
            floatArrayOf(0.68f, 0.68f, 0.68f),
            floatArrayOf(0.72f, 0.72f, 0.72f),
            floatArrayOf(0.72f, 0.60f, 0.50f)
        )
        for (s in samples) {
            val pixel = m.map(s[0], s[1], s[2])
            val codes = intArrayOf((pixel shr 16) and 255, (pixel shr 8) and 255, pixel and 255)
            for (c in 0 until 3) {
                val hdr = pqLinear(s[c])
                val base = srgbLinear(codes[c])
                val log = GainCodec.logOf(m.gainCodes[c]).toDouble()
                val rebuilt = (base + GainCodec.EPS) * exp(log) - GainCodec.EPS
                assertEquals(hdr, rebuilt, 0.01 * (hdr + GainCodec.EPS))
                assertTrue(log >= -0.02)
                assertTrue(log < GainCodec.LOG_HIGH - 0.5)
            }
        }
    }

    private companion object {
        const val M1 = 0.1593017578125
        const val M2 = 78.84375
        const val C1 = 0.8359375
        const val C2 = 18.8515625
        const val C3 = 18.6875
    }
}
