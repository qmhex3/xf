package com.frameout.core.export

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ArchiveSupportTest {
    private val file = File.createTempFile("frame", ".bin").apply { writeBytes(ByteArray(4)); deleteOnExit() }

    private fun frame(name: String, sequence: Int = 1) = StagedFrame(name, file, sequence.toLong(), sequence, sequence, 0L, 4, 4)

    private fun names(bytes: ByteArray): List<String> {
        val result = ArrayList<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) result += (zip.nextEntry ?: break).name
        }
        return result
    }

    @Test
    fun duplicateNamesGetNumericSuffixes() {
        val resolved = ArchiveNames.resolve(listOf(frame("a.jpg", 1), frame("a.jpg", 2), frame("A.JPG", 3)), emptySet())
        assertEquals(listOf("a.jpg", "a_2.jpg", "A_3.JPG"), resolved.map { it.name })
        assertEquals(3, resolved.map { it.name.lowercase() }.toSet().size)
    }

    @Test
    fun uniqueNamesAreKeptAsTheSameInstances() {
        val input = listOf(frame("a.jpg", 1), frame("b.jpg", 2))
        val resolved = ArchiveNames.resolve(input, emptySet())
        assertSame(input[0], resolved[0])
        assertSame(input[1], resolved[1])
    }

    @Test
    fun reservedNamesAreNeverReused() {
        val resolved = ArchiveNames.resolve(listOf(frame("metadata.csv")), setOf("metadata.csv"))
        assertEquals("metadata_2.csv", resolved.single().name)
    }

    @Test
    fun pathSeparatorsAreRemovedFromNames() {
        val resolved = ArchiveNames.resolve(listOf(frame("../x/y.jpg"), frame("a\\b.jpg")), emptySet())
        assertTrue(resolved.none { it.name.contains('/') || it.name.contains('\\') })
    }

    @Test
    fun resolvedFramesKeepEveryOtherField() {
        val renamed = frame("a.jpg", 7).withName("b.jpg")
        assertEquals("b.jpg", renamed.name)
        assertEquals(7, renamed.sequence)
        assertSame(file, renamed.file)
    }

    @Test
    fun entriesAreWrittenInCallOrderAndStreamIsClosed() {
        var closed = false
        val sink = object : ByteArrayOutputStream() {
            override fun close() { closed = true; super.close() }
        }
        ZipSession.use(sink) { zip ->
            ZipSession.putBytes(zip, "metadata.csv", ByteArray(1))
            ZipSession.putFrame(zip, frame("a.jpg"))
        }
        assertTrue(closed)
        assertEquals(listOf("metadata.csv", "a.jpg"), names(sink.toByteArray()))
    }

    @Test
    fun originalFailureSurvivesACloseFailure() {
        val broken = object : OutputStream() {
            override fun write(b: Int) = throw IOException("disk full")
            override fun close() = throw IOException("close failed")
        }
        val original = IllegalStateException("original")
        try {
            ZipSession.use(broken) { throw original }
            fail("expected failure")
        } catch (e: IllegalStateException) {
            assertSame(original, e)
        }
    }

    @Test
    fun writeFailureIsReportedNotSwallowed() {
        val broken = object : OutputStream() {
            override fun write(b: Int) = throw IOException("disk full")
            override fun write(b: ByteArray, off: Int, len: Int) = throw IOException("disk full")
        }
        var thrown = false
        try {
            ZipSession.use(broken) { zip -> ZipSession.putBytes(zip, "a.bin", ByteArray(100_000)) }
        } catch (e: IOException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun imageEntriesAreStoredAndOtherEntriesDeflated() {
        val sink = ByteArrayOutputStream()
        ZipSession.use(sink) { zip ->
            ZipSession.putFrame(zip, frame("a.jpg"))
            ZipSession.putFrame(zip, frame("b.WEBP", 2))
            ZipSession.putFrame(zip, frame("c.txt", 3))
        }
        val methods = LinkedHashMap<String, Int>()
        ZipInputStream(ByteArrayInputStream(sink.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                methods[entry.name] = entry.method
                assertEquals(4, zip.readBytes().size)
            }
        }
        assertEquals(ZipEntry.STORED, methods.getValue("a.jpg"))
        assertEquals(ZipEntry.STORED, methods.getValue("b.WEBP"))
        assertEquals(ZipEntry.DEFLATED, methods.getValue("c.txt"))
    }
}
