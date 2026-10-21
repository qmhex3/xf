package com.frameout.core.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatTest {
    private val edit = BackupEdit(90, true, false, 0.1f, 0.2f, 0.9f, 0.8f, 0.25f, 1.5f, 0.75f, false, "line\tone\nline two\\")
    private val project = BackupProject("Trip,\t\"Cairo\"\nday 1", "clip.mp4", "content://media/1", 61_000L, "EveryFrame", "jpg", 92, 0L, 61_000L, "Trip", 1_700_000_000_000L)
    private val data = BackupData(
        project,
        listOf(
            BackupFrame(0, 0L, true, false, false, null),
            BackupFrame(1, 33_333L, false, true, false, edit),
            BackupFrame(2, 66_666L, false, false, true, edit.copy(isDeleted = true, marks = ""))
        )
    )

    private fun invalid(text: String): BackupException.Reason =
        try { BackupManifest.decode(text); throw AssertionError("expected failure") } catch (e: BackupException) { e.reason }

    @Test
    fun manifestRoundTrips() = assertEquals(data, BackupManifest.decode(BackupManifest.encode(data)))

    @Test
    fun emptyProjectRoundTrips() {
        val empty = BackupData(project, emptyList())
        assertEquals(empty, BackupManifest.decode(BackupManifest.encode(empty)))
    }

    @Test
    fun escapeHandlesAllSpecials() {
        val raw = "a\tb\nc\rd\\e\\n"
        assertFalse(BackupText.escape(raw).any { it == '\t' || it == '\n' || it == '\r' })
        assertEquals(raw, BackupText.unescape(BackupText.escape(raw)))
    }

    @Test
    fun rejectsWrongMagicAndGarbage() {
        assertEquals(BackupException.Reason.Invalid, invalid("hello\n"))
        assertEquals(BackupException.Reason.Invalid, invalid(""))
        assertEquals(BackupException.Reason.Invalid, invalid("${BackupFormat.MAGIC}\tx\n"))
    }

    @Test
    fun rejectsNewerVersion() = assertEquals(BackupException.Reason.Newer, invalid("${BackupFormat.MAGIC}\t${BackupFormat.VERSION + 1}\nname\tx\nframes\nindex\tpts_us\n"))

    @Test
    fun rejectsMissingNameAndBadNumbers() {
        assertEquals(BackupException.Reason.Invalid, invalid("${BackupFormat.MAGIC}\t1\nframes\nindex\tpts_us\n"))
        assertEquals(BackupException.Reason.Invalid, invalid("${BackupFormat.MAGIC}\t1\nname\tx\nframes\nindex\tpts_us\nabc\t5\n"))
    }

    @Test
    fun unknownColumnsAndFieldsAreIgnored() {
        val text = "${BackupFormat.MAGIC}\t1\nname\tx\nfuture_field\tzzz\nframes\nindex\tpts_us\tfuture\n3\t9\tq\n"
        val parsed = BackupManifest.decode(text)
        assertEquals(listOf(BackupFrame(3, 9L, false, false, false, null)), parsed.frames)
    }

    @Test
    fun hostileEditValuesAreClamped() {
        val header = "${BackupFormat.MAGIC}\t1\nname\tx\nframes\nindex\tpts_us\thas_edit\trot\tcrop_l\tcrop_r\tbright\tcontrast\n"
        val parsed = BackupManifest.decode(header + "0\t5\t1\t45\t0.9\t0.1\tNaN\t99\n")
        val edit = parsed.frames.single().edit!!
        assertEquals(0, edit.rotation)
        assertEquals(0f, edit.cropL, 0f)
        assertEquals(1f, edit.cropR, 0f)
        assertEquals(0f, edit.brightness, 0f)
        assertEquals(1.5f, edit.contrast, 0f)
    }

    @Test
    fun negativeFrameIndexIsRejected() {
        assertEquals(BackupException.Reason.Invalid, invalid("${BackupFormat.MAGIC}\t1\nname\tx\nframes\nindex\tpts_us\n-1\t5\n"))
    }

    @Test
    fun archiveRoundTripsManifestAndThumbs() {
        val thumb = File.createTempFile("thumb", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4)); deleteOnExit() }
        val out = ByteArrayOutputStream()
        BackupWriter(out).apply { manifest(data); thumb(1, thumb); thumb(2, File("missing.jpg")); finish() }
        val reader = BackupReader(ByteArrayInputStream(out.toByteArray()))
        assertEquals(data, reader.manifest())
        val target = File.createTempFile("out", ".jpg").apply { deleteOnExit() }
        val written = reader.thumbs { target }
        assertEquals(setOf(1), written)
        assertEquals(listOf<Byte>(1, 2, 3, 4), target.readBytes().toList())
    }

    @Test
    fun readerRejectsNonArchives() {
        try { BackupReader(ByteArrayInputStream("not a zip".toByteArray())).manifest(); throw AssertionError("expected failure") } catch (e: BackupException) { assertEquals(BackupException.Reason.Invalid, e.reason) }
    }

    @Test
    fun namesAndEntries() {
        assertTrue(BackupFormat.isBackupName("a.FRAMEOUT"))
        assertFalse(BackupFormat.isBackupName("a.mp4"))
        assertFalse(BackupFormat.isBackupName(null))
        assertEquals(7, BackupFormat.thumbIndex("thumbs/7.jpg"))
        assertNull(BackupFormat.thumbIndex("thumbs/../7.jpg"))
        assertNull(BackupFormat.thumbIndex("x/7.jpg"))
        val name = BackupFormat.fileName("a/b:c*\"d", 0L)
        assertTrue(name.endsWith(".frameout"))
        assertFalse(name.any { it == '/' || it == ':' || it == '*' || it == '"' })
    }
}
