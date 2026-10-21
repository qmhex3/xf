package com.frameout.codecpack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerFormatSniffTest {
    private class Case(val ext: String, val hex: String, val container: String, val sync: List<Int> = emptyList())

    private val cases = listOf(
        Case("mkv", "1a45dfa3a34286810142f7810142f281", "matroska"),
        Case("webm", "1a45dfa39f4286810142f7810142f281", "matroska"),
        Case("avi", "52494646f6070200415649204c495354", "avi"),
        Case("wmv", "3026b2758e66cf11a6d900aa0062ce6c", "asf"),
        Case("flv", "464c5601050000000900000000120001", "flv"),
        Case("ts", "474011100042f0250001c10000ff01ff", "mpeg-ts", listOf(188, 376)),
        Case("m2ts", "0ebf4622474011100042f0250001c100", "mpeg-ts", listOf(196, 388)),
        Case("mts", "0ebf4622474011100042f0250001c100", "mpeg-ts", listOf(196, 388)),
        Case("vob", "000001ba44000400040143378bf80000", "mpeg-ps"),
        Case("3gp", "0000001c667479703367703400000200", "mp4"),
        Case("rmvb", "2e524d46000000120000000000000000", "realmedia"),
        Case("ogv", "4f676753000200000000000000004efa", "ogg")
    )

    private fun head(case: Case): ByteArray {
        val out = ByteArray(ContainerSniffer.HEAD_BYTES)
        case.hex.chunked(2).forEachIndexed { index, pair -> out[index] = pair.toInt(16).toByte() }
        case.sync.forEach { out[it] = 0x47 }
        return out
    }

    private fun isoHead(brand: String): ByteArray {
        val out = ByteArray(ContainerSniffer.HEAD_BYTES)
        "ftyp$brand".forEachIndexed { index, c -> out[4 + index] = c.code.toByte() }
        return out
    }

    @Test
    fun everyFormatHeaderIsSniffedAsVideo() {
        cases.forEach { case ->
            val sniffed = ContainerSniffer.sniff(head(case))
            assertNotNull("no container for ${case.ext}", sniffed)
            assertEquals(case.ext, case.container, sniffed!!.container)
            assertTrue("${case.ext} not video", sniffed.video)
        }
    }

    @Test
    fun everyFormatExtensionIsListedInAnyCase() {
        cases.forEach { case ->
            assertTrue(case.ext, case.ext in ContainerSniffer.videoExtensions)
            assertTrue(case.ext, ContainerSniffer.isVideoName("Clip.${case.ext.uppercase()}"))
        }
    }

    @Test
    fun threeGpBrandsAreVideo() {
        listOf("3gp4", "3gp5", "3gp6", "3g2a").forEach { brand ->
            val sniffed = ContainerSniffer.sniff(isoHead(brand))
            assertNotNull(brand, sniffed)
            assertTrue(brand, sniffed!!.video)
        }
    }

    @Test
    fun tsSyncAtZeroAndM2tsSyncAtFourAreBothAccepted() {
        val ts = head(cases.first { it.ext == "ts" })
        val m2ts = head(cases.first { it.ext == "m2ts" })
        assertEquals(0x47, ts[0].toInt())
        assertEquals(0x47, m2ts[4].toInt())
        assertEquals("mpeg-ts", ContainerSniffer.sniff(ts)?.container)
        assertEquals("mpeg-ts", ContainerSniffer.sniff(m2ts)?.container)
    }
}
