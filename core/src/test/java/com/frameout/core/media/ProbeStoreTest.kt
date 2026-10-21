package com.frameout.core.media

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProbeStoreTest {
    private fun file(): File = File.createTempFile("probe", ".bin").also { it.delete(); it.deleteOnExit() }

    @Test
    fun verdictsSurviveReopen() {
        val f = file()
        val first = ProbeStore.Space(f)
        first.put(1L, true)
        first.put(2L, false)
        first.flush()
        val second = ProbeStore.Space(f)
        assertEquals(true, second.get(1L))
        assertEquals(false, second.get(2L))
        assertNull(second.get(3L))
    }

    @Test
    fun lastWriteWinsAfterReopen() {
        val f = file()
        val first = ProbeStore.Space(f)
        first.put(7L, true)
        first.put(7L, false)
        first.flush()
        assertEquals(false, ProbeStore.Space(f).get(7L))
    }

    @Test
    fun truncatedTailIsIgnored() {
        val f = file()
        val first = ProbeStore.Space(f)
        first.put(5L, true)
        first.flush()
        f.appendBytes(byteArrayOf(1, 2, 3))
        val second = ProbeStore.Space(f)
        assertEquals(true, second.get(5L))
    }

    @Test
    fun hashDependsOnTheWholeKey() {
        assertNotEquals(ProbeStore.hash("uri|100|1"), ProbeStore.hash("uri|101|1"))
        assertEquals(ProbeStore.hash("uri|100|1"), ProbeStore.hash("uri|100|1"))
    }
}
