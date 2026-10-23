package com.frameout.core.pro

import com.frameout.core.storage.ImageFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProCatalogTest {
    @Test
    fun everyoneKeepsTheCoreFormats() {
        listOf(ImageFormat.Jpg, ImageFormat.Png, ImageFormat.WebpLossy).forEach { assertTrue(it.key, ProCatalog.formatIsFree(it)) }
    }

    @Test
    fun advancedFormatsNeedPro() {
        listOf(ImageFormat.JpgHdr, ImageFormat.PngHdr16, ImageFormat.Heic, ImageFormat.Avif)
            .forEach { assertFalse(it.key, ProCatalog.formatIsFree(it)) }
    }

    @Test
    fun freeFiltersAreOnlyTheBasicOnes() {
        assertTrue(ProCatalog.filterIsFree(""))
        assertTrue(ProCatalog.filterIsFree("sepia"))
        assertFalse(ProCatalog.filterIsFree("vivid"))
    }

    @Test
    fun freeCollagesAreTheThreeSimpleGrids() {
        assertEquals(setOf("2x1", "1x2", "2x2"), ProCatalog.freeCollages)
        assertFalse(ProCatalog.collageIsFree("3x3"))
    }

    @Test
    fun onlyIndigoAccentIsFree() {
        assertTrue(ProCatalog.accentIsFree("indigo"))
        assertFalse(ProCatalog.accentIsFree("rose"))
    }

    @Test
    fun featureKeysAreUnique() {
        val keys = ProFeature.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun trialAndGraceLimitsAreSane() {
        assertTrue(ProCatalog.TRIALS_PER_FEATURE in 1..10)
        assertTrue(ProCatalog.GRACE_DAYS in 1..60)
    }
}
