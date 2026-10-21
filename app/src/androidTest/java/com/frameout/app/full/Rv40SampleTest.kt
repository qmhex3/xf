package com.frameout.app.full

import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.codecpack.FfmpegBridge
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

@FullSuite
class Rv40SampleTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun realRv40RmvbDecodes() {
        val file = File(Fixtures.dir(context), "fmt_rv40.rmvb")
        assumeTrue("no real RV40 sample (set RV40_SAMPLE_URL when preparing fixtures)", file.isFile)
        assumeTrue("codec pack not loadable", DeviceCapabilities.ffmpegLoaded())
        val path = file.absolutePath
        val parts = FfmpegBridge.nativeProbe(path).split('|')
        assertEquals("probe output ${parts.joinToString("|")}", 4, parts.size)
        assertEquals("video/rv40", parts[0])
        val index = requireNotNull(FfmpegBridge.nativeFrameIndex(path))
        assertTrue("rmvb index too short: ${index.size / 4}", index.size / 4 > 30)
        try {
            listOf(0, 10, 29).forEach { position ->
                val frame = NativeFrames.decode(path, position, parts[1].toInt(), parts[2].toInt())
                assertEquals("decode rc for frame $position", 0, frame.rc)
                assertTrue("frame $position is opaque", frame.opaque)
            }
        } finally {
            FfmpegBridge.nativeCloseSession(path)
        }
    }
}
