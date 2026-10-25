package com.frameout.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageFormatTest {
    private val pro: (ImageFormat) -> Boolean = { true }

    @Test
    fun keysRoundTrip() {
        ImageFormat.entries.forEach { assertEquals(it, ImageFormat.fromKey(it.key)) }
    }

    @Test
    fun savedLosslessWebpKeyMeansWebp() {
        assertEquals(ImageFormat.WebpLossy, ImageFormat.fromKey("webp_lossless"))
        assertEquals(listOf("jpg", "jpg_hdr", "png", "png_hdr16", "webp", "heic", "avif"), ImageFormat.keys)
    }

    @Test
    fun unknownOrMissingKeyFallsBackToJpg() {
        assertEquals(ImageFormat.Jpg, ImageFormat.fromKey("bmp"))
        assertEquals(ImageFormat.Jpg, ImageFormat.fromKey(null))
        assertEquals(ImageFormat.Jpg, ImageFormat.fromKey(""))
    }

    @Test
    fun extensionsAndMimeTypes() {
        assertEquals("jpg" to "image/jpeg", ImageFormat.Jpg.ext to ImageFormat.Jpg.mime)
        assertEquals("jpg" to "image/jpeg", ImageFormat.JpgHdr.ext to ImageFormat.JpgHdr.mime)
        assertEquals("png" to "image/png", ImageFormat.Png.ext to ImageFormat.Png.mime)
        assertEquals("webp" to "image/webp", ImageFormat.WebpLossy.ext to ImageFormat.WebpLossy.mime)
        assertEquals("heic" to "image/heic", ImageFormat.Heic.ext to ImageFormat.Heic.mime)
        assertEquals("avif" to "image/avif", ImageFormat.Avif.ext to ImageFormat.Avif.mime)
    }

    @Test
    fun keysAreUniqueAndListed() {
        assertEquals(ImageFormat.entries.size, ImageFormat.keys.toSet().size)
        assertEquals(listOf("jpg", "jpg_hdr", "png", "png_hdr16", "webp", "heic", "avif"), ImageFormat.keys)
    }

    @Test
    fun losslessFormatsIgnoreQuality() {
        assertEquals(100, ImageFormat.Png.effectiveQuality(50))
    }

    @Test
    fun lossyFormatsClampQuality() {
        assertEquals(80, ImageFormat.Jpg.effectiveQuality(80))
        assertEquals(1, ImageFormat.WebpLossy.effectiveQuality(-5))
        assertEquals(100, ImageFormat.WebpLossy.effectiveQuality(400))
        assertEquals(60, ImageFormat.Heic.effectiveQuality(60))
        assertEquals(100, ImageFormat.Avif.effectiveQuality(400))
    }

    @Test
    fun chosenFormatIsNeverSilentlyReplacedByGainmap() {
        assertEquals(ImageFormat.Png, ImageFormat.resolve("png", true, 34, allowed = pro))
        assertEquals(ImageFormat.WebpLossy, ImageFormat.resolve("webp_lossless", true, 34, allowed = pro))
        assertEquals(ImageFormat.Jpg, ImageFormat.resolve("jpg", true, 34, allowed = pro))
    }

    @Test
    fun hdrFormatNeedsGainmapAndSupportedSdk() {
        assertEquals(ImageFormat.JpgHdr, ImageFormat.resolve("jpg_hdr", true, 34, allowed = pro))
        assertEquals(ImageFormat.Jpg, ImageFormat.resolve("jpg_hdr", false, 34, allowed = pro))
        assertEquals(ImageFormat.Jpg, ImageFormat.resolve("jpg_hdr", true, 33, allowed = pro))
    }

    @Test
    fun availabilityByPlatform() {
        assertFalse(ImageFormat.JpgHdr.isAvailable(33))
        assertTrue(ImageFormat.JpgHdr.isAvailable(34))
        assertFalse(ImageFormat.Heic.isAvailable(28))
        assertFalse(ImageFormat.Avif.isAvailable(28))
        assertEquals(listOf("jpg", "png", "webp"), ImageFormat.availableKeys(24))
    }

    @Test
    fun workingCopiesNeverUseModernEncoders() {
        assertEquals(ImageFormat.Jpg, ImageFormat.working("heic", false, 34, allowed = pro))
        assertEquals(ImageFormat.Jpg, ImageFormat.working("avif", true, 34, allowed = pro))
        assertEquals(ImageFormat.JpgHdr, ImageFormat.working("jpg_hdr", true, 34, allowed = pro))
        assertEquals(ImageFormat.Png, ImageFormat.working("png", false, 34, allowed = pro))
    }

    @Test
    fun onlyHdrFormatIsHdr() {
        assertEquals(listOf(ImageFormat.JpgHdr, ImageFormat.PngHdr16), ImageFormat.entries.filter { it.hdr })
        assertEquals(listOf(ImageFormat.Heic, ImageFormat.Avif), ImageFormat.entries.filter { it.modern })
    }

    @Test
    fun exactHdrFormatFallsBackByWhatTheFrameCarries() {
        assertEquals(ImageFormat.PngHdr16, ImageFormat.resolve("png_hdr16", true, 34, hasSignal = true, allowed = pro))
        assertEquals(ImageFormat.JpgHdr, ImageFormat.resolve("png_hdr16", true, 34, allowed = pro))
        assertEquals(ImageFormat.Png, ImageFormat.resolve("png_hdr16", false, 34, allowed = pro))
        assertEquals(ImageFormat.Jpg, ImageFormat.resolve("png_hdr16", true, 33, hasSignal = true, allowed = pro))
        assertEquals(100, ImageFormat.PngHdr16.effectiveQuality(10))
    }

    @Test
    fun freeTierKeepsOnlyFreeFormats() {
        val free: (ImageFormat) -> Boolean = { it in com.frameout.core.pro.ProCatalog.freeFormats }
        assertEquals(ImageFormat.Png, ImageFormat.resolve("png", true, 34, allowed = free))
        assertEquals(ImageFormat.WebpLossy, ImageFormat.resolve("webp_lossless", true, 34, allowed = free))
        assertEquals(ImageFormat.Jpg, ImageFormat.resolve("jpg_hdr", true, 34, allowed = free))
    }

    @Test
    fun webpAtFullQualityIsLosslessFromAndroid10() {
        assertEquals(true, ImageFormat.webpIsLossless(100, 29))
        assertEquals(true, ImageFormat.webpIsLossless(100, 34))
        assertEquals(false, ImageFormat.webpIsLossless(99, 34))
        assertEquals(false, ImageFormat.webpIsLossless(100, 28))
    }
}
