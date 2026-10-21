package com.frameout.app.full

import com.frameout.engine.decode.DecodeRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test

@FullSuite
class ConcurrentConsumersTest : FullSuiteBase() {
    private fun ffmpegFixture(): Fixture {
        assumeTrue("codec pack not loadable", DeviceCapabilities.ffmpegLoaded())
        val fixture = Fixtures.needingFfmpeg(context).maxByOrNull { it.frames }
        assumeTrue("no fixture that needs the codec pack", fixture != null)
        return fixture!!
    }

    @Test
    fun secondConsumerDoesNotDisturbRunningExtraction() = runBlocking {
        val fixture = ffmpegFixture()
        val id = project(fixture.file)
        coroutineScope {
            val extraction = async(Dispatchers.Default) { engine().run(request(fixture.file, id)) }
            val consumer = launch(Dispatchers.IO) {
                while (!extraction.isCompleted) {
                    decodeOnce(fixture)
                    delay(POLL_MS)
                }
            }
            extraction.await()
            consumer.cancelAndJoin()
        }
        val pts = db.frameDao().listPts(id)
        assertEquals("Completed", status(id))
        assertEquals(fixture.frames, pts.size)
        assertEquals(pts.size, pts.toSet().size)
    }

    @Test
    fun closingOneRouterKeepsTheOtherUsable() {
        val fixture = ffmpegFixture()
        val first = DecodeRouter(context)
        try {
            first.open(fixture.uri)
            assertNotNull(first.decodeFfmpegFrame(0)?.bitmap)
            decodeOnce(fixture)
            assertNotNull(first.decodeFfmpegFrame(1)?.bitmap)
            assertNotNull(first.decodeFfmpegFrame(0)?.bitmap)
        } finally {
            first.close()
        }
    }

    private fun decodeOnce(fixture: Fixture) {
        val router = DecodeRouter(context)
        try {
            router.open(fixture.uri)
            router.decodeFfmpegFrame(0)?.bitmap
        } finally {
            router.close()
        }
    }

    private companion object {
        const val POLL_MS = 20L
    }
}
