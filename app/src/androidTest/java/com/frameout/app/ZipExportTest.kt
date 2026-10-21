package com.frameout.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.core.export.EvidenceFrame
import com.frameout.core.export.EvidencePackage
import com.frameout.core.export.EvidenceSource
import com.frameout.core.export.FramesZip
import com.frameout.core.export.StagedFrame
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipInputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZipExportTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val files = mutableListOf<File>()

    @After
    fun cleanup() {
        files.forEach { it.delete() }
    }

    private fun staged(name: String, sequence: Int): StagedFrame {
        val file = File(context.cacheDir, "zip_${System.nanoTime()}.bin").also { files += it }
        file.writeBytes(ByteArray(64) { sequence.toByte() })
        return StagedFrame(name, file, sequence.toLong(), sequence, sequence, sequence * 1_000_000L, 8, 8)
    }

    private fun entryNames(bytes: ByteArray): List<String> {
        val names = ArrayList<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) names += (zip.nextEntry ?: break).name
        }
        return names
    }

    @Test
    fun repeatedNamesStillProduceAValidArchive() {
        val out = ByteArrayOutputStream()
        FramesZip.write(listOf(staged("f.jpg", 1), staged("f.jpg", 2), staged("F.jpg", 3)), "p", out)
        val names = entryNames(out.toByteArray())
        assertEquals(names.size, names.toSet().size)
        assertEquals(5, names.size)
    }

    @Test
    fun metadataComesBeforeTheImages() {
        val out = ByteArrayOutputStream()
        FramesZip.write(listOf(staged("a.jpg", 1), staged("b.jpg", 2)), "p", out)
        assertEquals(listOf(FramesZip.CSV_NAME, FramesZip.JSON_NAME, "a.jpg", "b.jpg"), entryNames(out.toByteArray()))
    }

    @Test
    fun evidenceArchiveSurvivesRepeatedNames() {
        val frames = listOf(staged("e.jpg", 1), staged("e.jpg", 2)).map { EvidenceFrame(it, EvidencePackage.sha256(it.file)) }
        val out = ByteArrayOutputStream()
        EvidencePackage.write(frames, "p", EvidenceSource("clip.mp4", null, 0L), 1_700_000_000_000L, "1.0", out)
        val names = entryNames(out.toByteArray())
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.containsAll(listOf("e.jpg", "e_2.jpg", EvidencePackage.SUMS_NAME)))
    }

    @Test
    fun streamFailureSurfacesToTheCaller() {
        val broken = object : OutputStream() {
            override fun write(b: Int) = throw IOException("disk full")
            override fun write(b: ByteArray, off: Int, len: Int) = throw IOException("disk full")
        }
        var thrown = false
        try {
            FramesZip.write(listOf(staged("a.jpg", 1)), "p", broken)
        } catch (e: IOException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
