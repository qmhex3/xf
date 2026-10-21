package com.frameout.core.export

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidencePackageTest {
    private fun frame(name: String, sequence: Int, hash: String): EvidenceFrame {
        val file = File.createTempFile("frame", ".bin").apply { writeBytes(ByteArray(5)); deleteOnExit() }
        return EvidenceFrame(StagedFrame(name, file, 1L, sequence, sequence, 1_500_000L, 10, 20), hash)
    }

    @Test
    fun hashesKnownVectors() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", EvidencePackage.sha256(ByteArray(0)))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", EvidencePackage.sha256("abc".toByteArray()))
    }

    @Test
    fun streamHashReportsLength() {
        val result = EvidencePackage.sha256("abc".byteInputStream())
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", result.first)
        assertEquals(3L, result.second)
    }

    @Test
    fun formatsUtcTimestamp() {
        assertEquals("2024-03-05T07:08:09Z", EvidencePackage.utc(1709622489000L))
    }

    @Test
    fun sumsUseTwoSpaceFormat() {
        val frames = listOf(frame("a.jpg", 1, "aa"), frame("b.jpg", 2, "bb"))
        assertEquals("aa  a.jpg\nbb  b.jpg\ncc  evidence.json\n", EvidencePackage.sums(frames, linkedMapOf("evidence.json" to "cc")))
    }

    @Test
    fun reportListsSourceAndFrameHashes() {
        val text = EvidencePackage.report("Case", EvidenceSource("clip.mp4", "ff00", 99L), listOf(frame("a.jpg", 1, "aa")), 1709622489000L, "1.0")
        assertTrue(text.contains("Source hash: ff00"))
        assertTrue(text.contains("2024-03-05T07:08:09Z"))
        assertTrue(text.contains("a.jpg"))
        assertTrue(text.contains("    aa"))
    }

    @Test
    fun reportMarksUnavailableSourceHash() {
        val text = EvidencePackage.report("Case", EvidenceSource("clip.mp4", null, 0L), emptyList(), 0L, "1.0")
        assertTrue(text.contains("Source hash: unavailable"))
    }
}
