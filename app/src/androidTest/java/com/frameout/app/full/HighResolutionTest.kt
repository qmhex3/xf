package com.frameout.app.full

import android.os.Debug
import com.frameout.codecpack.FrameBudget
import com.frameout.engine.extract.ExtractProgress
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

@FullSuite
class HighResolutionTest : FullSuiteBase() {
    @Test
    fun frameBudgetKeepsEightKWithinLimits() {
        val (w, h) = FrameBudget.fit(7680, 4320)!!
        assertTrue(w.toLong() * h <= FrameBudget.MAX_PIXELS)
        assertTrue(maxOf(w, h) <= FrameBudget.MAX_SIDE)
        assertEquals(7680.0 / 4320.0, w.toDouble() / h, 0.01)
        assertEquals(320 to 240, FrameBudget.fit(320, 240))
    }

    @Test
    fun uhdExtractionStaysWithinMemoryBudget() = runBlocking {
        assumeTrue("no 4K avc encoder", DeviceCapabilities.encoderSupportsSize("video/avc", WIDTH, HEIGHT))
        assumeTrue("needs 3GB ram", DeviceCapabilities.totalMemoryBytes(context) >= 3L * 1024 * MB)
        val file = generate(constant(FRAMES, WIDTH, HEIGHT))
        val truth = com.frameout.app.GeneratedVideo.sampleTimes(file)
        assumeTrue("encoder produced ${truth.size} samples", truth.size == FRAMES)
        val id = project(file)
        val peak = AtomicLong(0)
        val engine = engine()
        val watcher = launch(Dispatchers.Default) {
            engine.progress.collect { if (it.projectId == id && it.status == ExtractProgress.Status.Extracting) peak.accumulateAndGet(used()) { a, b -> maxOf(a, b) } }
        }
        engine.run(request(file, id))
        watcher.cancelAndJoin()
        assertEquals("Completed", status(id))
        assertEquals(FRAMES, db.frameDao().countAll(id))
        assertTrue("peak ${peak.get() / MB}MB", peak.get() <= BUDGET_MB * MB)
    }

    private fun used(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory() + Debug.getNativeHeapAllocatedSize()
    }

    private companion object {
        const val MB = 1024L * 1024L
        const val WIDTH = 3840
        const val HEIGHT = 2160
        const val FRAMES = 6
        const val BUDGET_MB = 700L
    }
}
