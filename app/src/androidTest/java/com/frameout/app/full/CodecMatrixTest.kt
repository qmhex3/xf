package com.frameout.app.full

import android.os.Build
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@FullSuite
@RunWith(Parameterized::class)
class CodecMatrixTest(private val mime: String, private val minApi: Int) : FullSuiteBase() {
    @Test
    fun everyFrameIsExtractedForCodec() = runBlocking {
        assumeTrue("api ${Build.VERSION.SDK_INT} below $minApi", Build.VERSION.SDK_INT >= minApi)
        assumeTrue("no encoder for $mime", DeviceCapabilities.hasEncoder(mime))
        assumeTrue("no decoder for $mime", DeviceCapabilities.hasDecoder(mime))
        val spec = constant(FRAMES, mime = mime)
        val file = generate(spec)
        val truth = com.frameout.app.GeneratedVideo.sampleTimes(file)
        assumeTrue("encoder produced ${truth.size} samples", truth.size == FRAMES)
        val id = extract(file)
        val pts = db.frameDao().listPts(id)
        assertEquals("Completed", status(id))
        assertEquals(FRAMES, pts.size)
        assertEquals(pts.size, pts.toSet().size)
        assertEquals(relative(truth), relative(pts))
        assertTrue(pts == pts.sorted())
    }

    companion object {
        private const val FRAMES = 30

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun codecs(): List<Array<Any>> = listOf(
            arrayOf("video/avc", 24),
            arrayOf("video/hevc", 24),
            arrayOf("video/x-vnd.on2.vp8", 24),
            arrayOf("video/x-vnd.on2.vp9", 24),
            arrayOf("video/av01", 34)
        )
    }
}
