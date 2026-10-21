package com.frameout.codecpack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerSnifferTest {
    private fun bytes(vararg v: Int): ByteArray = ByteArray(ContainerSniffer.HEAD_BYTES).also { out -> v.forEachIndexed { i, b -> out[i] = b.toByte() } }

    private fun ascii(offset: Int, text: String, base: ByteArray = ByteArray(ContainerSniffer.HEAD_BYTES)): ByteArray {
        text.forEachIndexed { i, c -> base[offset + i] = c.code.toByte() }
        return base
    }

    @Test
    fun matroskaAndWebmAreDetected() {
        assertEquals("matroska", ContainerSniffer.sniff(bytes(0x1A, 0x45, 0xDF, 0xA3))?.container)
    }

    @Test
    fun aviIsSeparatedFromWebp() {
        val avi = ascii(8, "AVI ", ascii(0, "RIFF"))
        val webp = ascii(8, "WEBP", ascii(0, "RIFF"))
        assertEquals("avi", ContainerSniffer.sniff(avi)?.container)
        assertTrue(ContainerSniffer.sniff(avi)!!.video)
        assertFalse(ContainerSniffer.sniff(webp)!!.video)
    }

    @Test
    fun isoBrandsMapToContainers() {
        assertEquals("mov", ContainerSniffer.sniff(ascii(8, "qt  ", ascii(4, "ftyp")))?.container)
        assertEquals("avif", ContainerSniffer.sniff(ascii(8, "avif", ascii(4, "ftyp")))?.container)
        assertEquals("mp4", ContainerSniffer.sniff(ascii(8, "isom", ascii(4, "ftyp")))?.container)
        assertFalse(ContainerSniffer.sniff(ascii(8, "heic", ascii(4, "ftyp")))!!.video)
    }

    @Test
    fun transportStreamNeedsRepeatedSyncBytes() {
        val ts = ByteArray(ContainerSniffer.HEAD_BYTES)
        ts[0] = 0x47; ts[188] = 0x47; ts[376] = 0x47
        assertEquals("mpeg-ts", ContainerSniffer.sniff(ts)?.container)
        val lone = ByteArray(ContainerSniffer.HEAD_BYTES)
        lone[0] = 0x47
        assertNull(ContainerSniffer.sniff(lone))
    }

    @Test
    fun m2tsUsesFourByteTimestampPrefix() {
        val m2ts = ByteArray(ContainerSniffer.HEAD_BYTES)
        m2ts[4] = 0x47; m2ts[196] = 0x47; m2ts[388] = 0x47
        assertEquals("mpeg-ts", ContainerSniffer.sniff(m2ts)?.container)
    }

    @Test
    fun otherSignatures() {
        assertEquals("flv", ContainerSniffer.sniff(bytes('F'.code, 'L'.code, 'V'.code, 1))?.container)
        assertEquals("asf", ContainerSniffer.sniff(bytes(0x30, 0x26, 0xB2, 0x75, 0x8E, 0x66, 0xCF, 0x11))?.container)
        assertEquals("mpeg-ps", ContainerSniffer.sniff(bytes(0x00, 0x00, 0x01, 0xBA))?.container)
        assertEquals("ogg", ContainerSniffer.sniff(ascii(0, "OggS"))?.container)
        assertEquals("realmedia", ContainerSniffer.sniff(ascii(0, ".RMF"))?.container)
        assertEquals("gif", ContainerSniffer.sniff(ascii(0, "GIF89a"))?.container)
    }

    @Test
    fun rareVideoContainersAreRecognised() {
        val mxf = bytes(0x06, 0x0E, 0x2B, 0x34, 0x02, 0x05, 0x01, 0x01, 0x0D, 0x01, 0x02, 0x01)
        assertEquals("mxf", ContainerSniffer.sniff(mxf)?.container)
        assertTrue(ContainerSniffer.sniff(mxf)!!.video)
        val nut = ascii(0, "nut/multimedia container")
        assertEquals("nut", ContainerSniffer.sniff(nut)?.container)
        assertTrue(ContainerSniffer.sniff(nut)!!.video)
        listOf("rm", "rmvb", "mxf", "ts", "m2ts", "mts", "nut").forEach { assertTrue(it, it in ContainerSniffer.videoExtensions) }
    }

    @Test
    fun garbageAndShortInputAreNull() {
        assertNull(ContainerSniffer.sniff(ByteArray(3)))
        assertNull(ContainerSniffer.sniff(ByteArray(ContainerSniffer.HEAD_BYTES) { 0x20 }))
    }

    @Test
    fun extensionsAreCaseInsensitive() {
        assertTrue(ContainerSniffer.isVideoName("Holiday.MKV"))
        assertTrue(ContainerSniffer.isVideoName("clip.ts"))
        assertFalse(ContainerSniffer.isVideoName("notes.txt"))
        assertNotNull(ContainerSniffer.videoExtensions.firstOrNull { it == "mxf" })
    }

    @Test
    fun rawDvNeedsHeaderAndSubcodeBlocks() {
        val pal = bytes(0x1F, 0x07, 0x00, 0xBF).also { it[80] = 0x3F }
        val ntsc = bytes(0x1F, 0x07, 0x00, 0x3F).also { it[80] = 0x3F }
        assertEquals("dv", ContainerSniffer.sniff(pal)?.container)
        assertEquals("dv", ContainerSniffer.sniff(ntsc)?.container)
        assertNull(ContainerSniffer.sniff(bytes(0x1F, 0x07, 0x00, 0xBF)))
    }

    @Test
    fun motionJpeg2000BrandIsVideo() {
        val sniffed = ContainerSniffer.sniff(ascii(8, "mjp2", ascii(4, "ftyp")))
        assertEquals("mj2", sniffed?.container)
        assertTrue(sniffed!!.video)
        assertTrue(ContainerSniffer.isVideoName("clip.mj2"))
        assertTrue(ContainerSniffer.isVideoName("clip.dnxhd"))
    }
}
