package com.frameout.app.full

import com.frameout.core.util.DiagLog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

@FullSuite
class CorruptSourceTest : FullSuiteBase() {
    private fun diag(): String {
        DiagLog.flush(3_000L)
        return DiagLog.export(context).readText()
    }

    private suspend fun extractDamaged(damage: (ByteArray) -> ByteArray): Pair<Long, String> {
        assumeTrue("no avc encoder", DeviceCapabilities.hasEncoder("video/avc"))
        val file = generate(constant(30))
        file.writeBytes(damage(file.readBytes()))
        val id = project(file)
        runCatching { engine().run(request(file, id)) }
        return id to diag()
    }

    @Test
    fun truncatedVideoFailsInsteadOfCompleting() = runBlocking {
        val (id, log) = extractDamaged { it.copyOf(it.size * 3 / 10) }
        assertEquals("Failed", status(id))
        assertEquals(0, db.frameDao().countAll(id))
        assertTrue("extract failure missing from the diagnostic log", log.contains("[extract]"))
    }

    @Test
    fun destroyedHeaderFailsInsteadOfCompleting() = runBlocking {
        val (id, log) = extractDamaged { bytes -> bytes.also { for (i in 0 until minOf(64, it.size)) it[i] = 0 } }
        assertEquals("Failed", status(id))
        assertEquals(0, db.frameDao().countAll(id))
        assertTrue("extract failure missing from the diagnostic log", log.contains("[extract]"))
    }
}
