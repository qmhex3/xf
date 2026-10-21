package com.frameout.engine

import android.media.MediaFormat
import com.frameout.engine.decode.ColorParams
import com.frameout.engine.decode.GainCodec
import com.frameout.engine.decode.HdrToneMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HdrToneMapperTest {
    private fun pq(peak: Float = 1000f) = HdrToneMapper(ColorParams(true, true, MediaFormat.COLOR_TRANSFER_ST2084, peak))
    private fun hlg() = HdrToneMapper(ColorParams(true, true, MediaFormat.COLOR_TRANSFER_HLG, 1000f))
    private fun red(argb: Int) = (argb shr 16) and 255
    private fun green(argb: Int) = (argb shr 8) and 255
    private fun blue(argb: Int) = argb and 255

    @Test
    fun grayStaysNeutral() {
        val mapper = pq()
        listOf(0.1f, 0.3f, 0.5f, 0.58f, 0.7f, 0.9f).forEach {
            val p = mapper.map(it, it, it)
            assertTrue(kotlin.math.abs(red(p) - green(p)) <= 1)
            assertTrue(kotlin.math.abs(green(p) - blue(p)) <= 1)
        }
    }

    @Test
    fun blackStaysBlack() {
        val p = pq().map(0f, 0f, 0f)
        assertEquals(0, red(p))
        assertEquals(0, green(p))
        assertEquals(0, blue(p))
    }

    @Test
    fun brightnessIsMonotonic() {
        listOf(pq(), hlg()).forEach { mapper ->
            var last = -1
            for (i in 0..100) {
                val v = i / 100f
                val level = green(mapper.map(v, v, v))
                assertTrue(level >= last)
                last = level
            }
        }
    }

    @Test
    fun diffuseWhiteStaysBright() {
        val level = green(pq().map(0.58f, 0.58f, 0.58f))
        assertTrue(level in 225..250)
    }

    @Test
    fun peakMapsToWhite() {
        val p = pq().map(1f, 1f, 1f)
        assertEquals(255, red(p))
        assertEquals(255, green(p))
        assertEquals(255, blue(p))
    }

    @Test
    fun saturatedColorKeepsDominantChannel() {
        val p = pq().map(0.75f, 0.35f, 0.35f)
        assertTrue(red(p) > green(p))
        assertTrue(red(p) > blue(p))
    }

    @Test
    fun perChannelGainRebuildsHdrFromBaseWithinOneCode() {
        val mapper = HdrToneMapper(ColorParams(true, true, MediaFormat.COLOR_TRANSFER_ST2084, 1000f, primaries = 0), trackGain = true)
        val samples = listOf(floatArrayOf(0.4f, 0.4f, 0.4f), floatArrayOf(0.9f, 0.9f, 0.9f), floatArrayOf(0.75f, 0.45f, 0.40f), floatArrayOf(0.6f, 0.55f, 0.3f))
        for (s in samples) {
            val pixel = mapper.map(s[0], s[1], s[2])
            val codes = intArrayOf((pixel shr 16) and 255, (pixel shr 8) and 255, pixel and 255)
            for (c in 0 until 3) {
                val hdr = pqLinear(s[c])
                val base = srgbLinear(codes[c])
                val rebuilt = (base + GainCodec.EPS) * Math.exp(GainCodec.logOf(mapper.gainCodes[c]).toDouble()).toFloat() - GainCodec.EPS
                assertEquals(hdr, rebuilt, 0.01f * (hdr + GainCodec.EPS))
            }
        }
    }

    @Test
    fun gainGridKeepsUnityExact() {
        val grid = GainCodec.Grid(GainCodec.codeOf(-0.3f), GainCodec.codeOf(1.4f))
        assertEquals(grid.zero, grid.byteOf(GainCodec.codeOf(0f)))
        assertEquals(1f, Math.exp((grid.step * (grid.zero - grid.zero)).toDouble()).toFloat(), 0f)
        assertTrue(grid.ratioMin < 1f && grid.ratioMax > 1f)
    }

    private fun pqLinear(v: Float): Float {
        val m1 = 0.1593017578125; val m2 = 78.84375; val c1 = 0.8359375; val c2 = 18.8515625; val c3 = 18.6875
        val p = Math.pow(v.toDouble(), 1.0 / m2)
        val num = Math.max(p - c1, 0.0)
        return (10000.0 * Math.pow(num / (c2 - c3 * p), 1.0 / m1) / 203.0).toFloat()
    }

    private fun srgbLinear(code: Int): Float {
        val v = code / 255.0
        return (if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)).toFloat()
    }

    @Test
    fun toneCurveReachesWhiteAtPeak() {
        val mapper = pq(1000f)
        assertEquals(1f, mapper.toneLuma(1000f / 203f), 0.001f)
        assertEquals(0.4f, mapper.toneLuma(0.4f), 0.0001f)
    }
}
