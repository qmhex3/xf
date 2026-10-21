package com.frameout.app.full

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@FullSuite
@RunWith(Parameterized::class)
class RotationTest(private val rotation: Int) : FullSuiteBase() {
    @Test
    fun rotatedVideoExtractsEveryFrame() = runBlocking {
        assumeTrue("no avc encoder", DeviceCapabilities.hasEncoder("video/avc"))
        val file = generate(constant(FRAMES, rotation = rotation))
        val truth = com.frameout.app.GeneratedVideo.sampleTimes(file)
        assumeTrue("encoder produced ${truth.size} samples", truth.size == FRAMES)
        val id = extract(file)
        assertEquals("Completed", status(id))
        assertEquals(FRAMES, db.frameDao().countAll(id))
    }

    companion object {
        private const val FRAMES = 20

        @JvmStatic
        @Parameterized.Parameters(name = "rotation{0}")
        fun rotations(): List<Int> = listOf(90, 180, 270)
    }
}
