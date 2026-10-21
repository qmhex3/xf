package com.frameout.codecpack

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CodecPackLicensesTest {
    @get:Rule val tmp = TemporaryFolder()

    private val payload = "Frame out codec pack\nFFmpeg 9.0.2\nlibdav1d 1.5.4 (static, BSD-2-Clause)\nSerial: 20261006\nABIs: arm64-v8a\nLicense: LGPLv2.1 or later\n"

    private fun zipOf(vararg names: String): ZipFile {
        val file = tmp.newFile()
        ZipOutputStream(file.outputStream()).use { out ->
            names.forEach { name -> out.putNextEntry(ZipEntry(name)); out.write(1); out.closeEntry() }
        }
        return ZipFile(file)
    }

    @Test fun parsesVersionsSerialAndTexts() {
        val info = CodecPackLicenses.parse(payload, mapOf("COPYING.LGPLv2.1" to "lgpl", "COPYING.dav1d" to "bsd"), true)
        assertTrue(info.installed)
        assertEquals(20261006L, info.serial)
        assertEquals(listOf("FFmpeg", "dav1d"), info.licenses.map { it.name })
        assertEquals(listOf("LGPL-2.1-or-later", "BSD-2-Clause"), info.licenses.map { it.spdx })
        assertEquals(listOf("9.0.2", "1.5.4"), info.licenses.map { it.version })
        assertEquals(listOf("lgpl", "bsd"), info.licenses.map { it.text })
    }

    @Test fun missingPackKeepsNamesButNoDetails() {
        val info = CodecPackLicenses.parse(null, emptyMap(), false)
        assertFalse(info.installed)
        assertNull(info.serial)
        assertEquals(2, info.licenses.size)
        assertTrue(info.licenses.all { it.version == null && it.text == null })
    }

    @Test fun paragraphsJoinWrappedLines() {
        val text = "GNU LESSER\n  GENERAL PUBLIC LICENSE\n\n\nVersion 2.1,\nFebruary 1999\n"
        assertEquals(listOf("GNU LESSER GENERAL PUBLIC LICENSE", "Version 2.1, February 1999"), CodecPackLicenses.paragraphs(text))
    }

    @Test fun licenseFilesMustMatchSignedHashes() {
        val dir = tmp.newFolder()
        File(dir, "COPYING.LGPLv2.1").writeText("lgpl")
        File(dir, "COPYING.dav1d").writeText("bsd")
        val lines = listOf("COPYING.LGPLv2.1", "COPYING.dav1d").joinToString("\n") { "${CodecPackVerifier.sha256(File(dir, it))}  $it" }
        assertTrue(CodecPackVerifier.verifyLicenseFiles(payload + lines + "\n", dir))
        File(dir, "COPYING.dav1d").writeText("tampered")
        assertFalse(CodecPackVerifier.verifyLicenseFiles(payload + lines + "\n", dir))
        assertFalse(CodecPackVerifier.verifyLicenseFiles(payload, dir))
    }

    @Test fun archiveAcceptsBothCopyingFiles() {
        assertTrue(CodecPackVerifier.verifyArchive(zipOf("COPYING.LGPLv2.1", "COPYING.dav1d", "MANIFEST.txt", "arm64-v8a/SHA256SUMS")))
    }

    @Test fun archiveRejectsUnknownTopLevelFile() {
        assertFalse(CodecPackVerifier.verifyArchive(zipOf("COPYING.LGPLv2.1", "COPYING.other")))
    }
}
