package com.frameout.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryProbeRulesTest {
    private fun marks(selection: String) = selection.count { it == '?' }

    @Test
    fun everyPlaceholderHasAnArgumentOnBothStorageModels() {
        listOf(28, 29, 33).forEach { sdk ->
            val filter = MediaStoreMediaQuery.buildSpecialImages(sdk)
            assertEquals("sdk $sdk", marks(filter.selection), filter.args.size)
        }
    }

    @Test
    fun scopedStorageUsesRelativePathAndLegacyUsesDataPath() {
        val modern = MediaStoreMediaQuery.buildSpecialImages(30)
        assertTrue(modern.selection.contains("relative_path"))
        assertTrue("DCIM/%" in modern.args && "Pictures/%" in modern.args && "Download/%" in modern.args)
        val legacy = MediaStoreMediaQuery.buildSpecialImages(28)
        assertTrue(legacy.selection.contains("_data"))
        assertFalse(legacy.selection.contains("relative_path"))
        assertTrue("%/DCIM/%" in legacy.args && "%/Pictures/%" in legacy.args && "%/Download/%" in legacy.args)
    }

    @Test
    fun pngFilterIsBoundedBySizeAndSkipsScreenshots() {
        val filter = MediaStoreMediaQuery.buildSpecialImages(33)
        assertTrue("image/png" in filter.args)
        assertTrue(GalleryProbeRules.PNG_MIN_BYTES.toString() in filter.args)
        assertTrue(GalleryProbeRules.PNG_MAX_BYTES.toString() in filter.args)
        assertTrue("%Screenshot%" in filter.args)
        assertTrue(filter.selection.contains("NOT LIKE"))
        assertTrue(GalleryProbeRules.PNG_MIN_BYTES < GalleryProbeRules.PNG_MAX_BYTES)
    }

    @Test
    fun jpegOutsideCameraFolderNeedsMotionSizeFloor() {
        val filter = MediaStoreMediaQuery.buildSpecialImages(33)
        assertTrue(GalleryProbeRules.MOTION_EXTRA_MIN_BYTES.toString() in filter.args)
        assertEquals(2, filter.args.count { it == "image/jpeg" })
    }

    @Test
    fun keepsTheOriginalGifHeicWebpAndNameRules() {
        val filter = MediaStoreMediaQuery.buildSpecialImages(33)
        listOf("image/gif", "image/heic", "image/heif", "image/webp", "image/apng", "mvimg%", "%.mp.%", "%.apng").forEach { assertTrue(it, it in filter.args) }
    }
}
