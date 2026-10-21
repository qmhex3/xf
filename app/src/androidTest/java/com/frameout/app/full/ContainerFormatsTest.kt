package com.frameout.app.full

import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.codecpack.ContainerSniffer
import com.frameout.codecpack.FfmpegBridge
import com.frameout.codecpack.FormatKind
import com.frameout.codecpack.FormatProbe
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@FullSuite
@RunWith(Parameterized::class)
class ContainerFormatsTest(private val ext: String) {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val case = FormatCases.byExt(ext)

    private fun fixture(): Fixture {
        assumeTrue("no fixtures pushed to device", Fixtures.all(context).isNotEmpty())
        val found = Fixtures.find(context, case.fixture)
        assertNotNull("fixture ${case.fixture} is missing from the manifest", found)
        return found!!
    }

    private fun requireFfmpeg() = assumeTrue("codec pack not loadable", DeviceCapabilities.ffmpegLoaded())

    @Test
    fun formatIsDetected() {
        val fixture = fixture()
        val head = ByteArray(ContainerSniffer.HEAD_BYTES)
        val read = fixture.file.inputStream().use { it.read(head) }
        val sniffed = ContainerSniffer.sniff(head, read)
        assertNotNull("magic bytes of ${fixture.name} not recognised", sniffed)
        assertTrue("${fixture.name} sniffed as non video", sniffed!!.video)
        assertTrue(ContainerSniffer.isVideoName(fixture.name))
        val info = FormatProbe.probe(context, fixture.uri)
        val routed = info.kind == FormatKind.System || info.kind == FormatKind.NeedsFfmpeg
        assertTrue("${fixture.name} routed to ${info.kind}: ${info.message}", routed)
    }

    @Test
    fun containerOpensInFfmpeg() {
        val fixture = fixture()
        requireFfmpeg()
        val path = fixture.file.absolutePath
        val parts = FfmpegBridge.nativeProbe(path).split('|')
        assertEquals("probe output ${parts.joinToString("|")} for ${fixture.name}", 4, parts.size)
        assertEquals("video/${case.codec}", parts[0])
        assertEquals(case.width, parts[1].toInt())
        assertEquals(case.height, parts[2].toInt())
        assertEquals(case.displayWidth, FfmpegBridge.nativeWidth(path))
        assertEquals(case.displayHeight, FfmpegBridge.nativeHeight(path))
        assertEquals(fixture.frames, FfmpegBridge.nativeFrameCount(path))
    }

    @Test
    fun frameIndexIsCompleteAndOrdered() {
        val fixture = fixture()
        requireFfmpeg()
        val index = FfmpegBridge.nativeFrameIndex(fixture.file.absolutePath)
        assertNotNull("no frame index for ${fixture.name}", index)
        val pts = (0 until index!!.size / 4).map { index[it * 4] }
        assertEquals(fixture.frames, pts.size)
        assertEquals(pts.size, pts.toSet().size)
        assertTrue("pts order for ${fixture.name}", pts == pts.sorted())
        assertTrue("negative pts for ${fixture.name}", pts.all { it >= 0 })
    }

    @Test
    fun framesAreExtracted() {
        val fixture = fixture()
        requireFfmpeg()
        val path = fixture.file.absolutePath
        val index = requireNotNull(FfmpegBridge.nativeFrameIndex(path))
        val last = index.size / 4 - 1
        val tolerance = NativeFrames.halfFrameUs(index)
        try {
            listOf(0, last / 2, last).forEach { position ->
                val frame = NativeFrames.decode(path, position, case.displayWidth, case.displayHeight)
                val label = "${fixture.name} frame $position"
                assertEquals("decode rc for $label", 0, frame.rc)
                assertTrue("$label pts ${frame.ptsUs} vs index ${index[position * 4]}", abs(frame.ptsUs - index[position * 4]) < tolerance)
                assertTrue("$label is blank", frame.colors >= NativeFrames.MIN_COLORS)
                assertTrue("$label has transparent pixels", frame.opaque)
            }
        } finally {
            FfmpegBridge.nativeCloseSession(path)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun formats(): List<String> = FormatCases.required.map { it.ext }
    }
}
