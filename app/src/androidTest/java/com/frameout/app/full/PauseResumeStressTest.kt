package com.frameout.app.full

import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.engine.extract.ExtractProgress
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

@FullSuite
class PauseResumeStressTest : FullSuiteBase() {
    private val seed = InstrumentationRegistry.getArguments().getString("seed")?.toLongOrNull() ?: 7L

    @Test
    fun repeatedCancelAndResumeNeverLosesOrDuplicatesFrames() = runBlocking {
        assumeTrue("no avc encoder", DeviceCapabilities.hasEncoder("video/avc"))
        val file = generate(constant(FRAMES))
        val truth = com.frameout.app.GeneratedVideo.sampleTimes(file)
        assumeTrue("encoder produced ${truth.size} samples", truth.size == FRAMES)
        val id = project(file)
        val random = Random(seed)
        var previous = 0
        repeat(ROUNDS) {
            cancelAfter(id, file, random.nextInt(2, 25))
            val count = db.frameDao().countAll(id)
            assertTrue("count $count dropped below $previous", count >= previous)
            assertTrue("count $count above total", count <= FRAMES)
            previous = count
        }
        engine().run(request(file, id))
        val pts = db.frameDao().listPts(id)
        assertEquals(FRAMES, pts.size)
        assertEquals(pts.size, pts.toSet().size)
        assertEquals(relative(truth), relative(pts))
        assertEquals("Completed", status(id))
    }

    private suspend fun cancelAfter(id: Long, file: java.io.File, threshold: Int) = kotlinx.coroutines.coroutineScope {
        val engine = engine()
        val watcher = launch(Dispatchers.Default) {
            engine.progress.collect { if (it.projectId == id && it.status == ExtractProgress.Status.Extracting && it.done >= threshold) engine.cancel() }
        }
        runCatching { engine.run(request(file, id)) }
        watcher.cancelAndJoin()
    }

    private companion object {
        const val FRAMES = 120
        const val ROUNDS = 4
    }
}
