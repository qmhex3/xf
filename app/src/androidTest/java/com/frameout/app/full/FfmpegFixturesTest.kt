package com.frameout.app.full

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@FullSuite
@RunWith(Parameterized::class)
class FfmpegFixturesTest(private val name: String) : FullSuiteBase() {
    @Test
    fun fixtureExtractsEveryFrame() = runBlocking {
        val fixture = Fixtures.find(context, name)
        assumeTrue("no fixture $name pushed to device", fixture != null)
        assumeTrue("codec pack not loadable", DeviceCapabilities.ffmpegLoaded())
        val id = project(fixture!!.file)
        engine().run(request(fixture.file, id))
        val pts = db.frameDao().listPts(id)
        assertEquals("status for $name", "Completed", status(id))
        assertEquals("frames for $name", fixture.frames, pts.size)
        assertEquals(pts.size, pts.toSet().size)
        assertTrue("pts order for $name", pts == pts.sorted())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun fixtures(): List<String> = Fixtures.names().ifEmpty { listOf("none") }
    }
}
