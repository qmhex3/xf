package com.frameout.core.util

import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagLogTest {
    private val dir = File.createTempFile("diag", "").apply { delete(); mkdirs(); deleteOnExit() }

    @After
    fun cleanup() {
        DiagLog.reset()
        dir.deleteRecursively()
    }

    @Test
    fun redactsContentUris() {
        assertEquals("open failed content://<redacted> now", DiagRedactor.redact("open failed content://media/external/video/media/42 now"))
    }

    @Test
    fun redactsFileUrisAndPaths() {
        val text = DiagRedactor.redact("file:///storage/emulated/0/DCIM/Trip.mp4 and /data/user/0/app/cache/a.bin")
        assertFalse(text.contains("DCIM"))
        assertFalse(text.contains("/data/user"))
    }

    @Test
    fun redactsBareMediaNamesButKeepsExtension() {
        assertEquals("cannot read <file>.mp4", DiagRedactor.redact("cannot read holiday_clip.mp4"))
    }

    @Test
    fun leavesOrdinaryMessagesAlone() {
        val text = "IllegalStateException: decoder stalled at 12/30 (Scanner.kt:44)"
        assertEquals(text, DiagRedactor.redact(text))
    }

    @Test
    fun redactionIsIdempotent() {
        val once = DiagRedactor.redact("content://a/b/c.mp4 /sdcard/x/y.jpg z.png")
        assertEquals(once, DiagRedactor.redact(once))
    }

    @Test
    fun appendedLinesReachTheFile() {
        val file = File(dir, "log.txt")
        DiagLog.attach(DiagFileSink(file))
        repeat(50) { DiagLog.append("t", "line $it") }
        assertTrue(DiagLog.flush(5_000))
        assertEquals(50, file.readLines().size)
    }

    @Test
    fun uriInLogLineNeverReachesDisk() {
        val file = File(dir, "log.txt")
        DiagLog.attach(DiagFileSink(file))
        DiagLog.append("t", "failed content://media/external/video/media/42")
        DiagLog.flush(5_000)
        assertFalse(file.readText().contains("media/42"))
    }

    @Test
    fun appendAndFlushDoNotWaitForABlockedDisk() {
        val release = CountDownLatch(1)
        DiagLog.attach(object : DiagSink {
            override fun write(batch: List<String>) { release.await(10, TimeUnit.SECONDS) }
            override fun tail(maxBytes: Int) = ""
        })
        val started = System.nanoTime()
        DiagLog.append("t", "first")
        DiagLog.append("t", "second")
        val flushed = DiagLog.flush(200)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        release.countDown()
        assertFalse(flushed)
        assertTrue("took ${elapsedMs}ms", elapsedMs < 2_000)
    }

    @Test
    fun concurrentAppendsAreAllWritten() {
        val file = File(dir, "log.txt")
        DiagLog.attach(DiagFileSink(file))
        val workers = List(8) { id -> thread { repeat(100) { DiagLog.append("w$id", "n $it") } } }
        workers.forEach { it.join() }
        assertTrue(DiagLog.flush(5_000))
        assertEquals(800, file.readLines().size)
    }

    @Test
    fun fileRotatesPastTheLimit() {
        val file = File(dir, "log.txt")
        DiagFileSink(file, 100L).write(List(10) { "x".repeat(30) })
        assertTrue(File(dir, "log.prev.log").exists())
        assertFalse(file.exists())
    }

    @Test
    fun tailDropsTheCutLeadingLine() {
        val sink = DiagFileSink(File(dir, "log.txt"))
        sink.write(listOf("alpha-alpha", "beta", "gamma"))
        assertEquals("beta\ngamma\n", sink.tail(12))
    }
}
