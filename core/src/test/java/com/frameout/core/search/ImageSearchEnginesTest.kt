package com.frameout.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageSearchEnginesTest {
    @Test
    fun idsAreUniqueAndUrlsSecure() {
        val ids = ImageSearchEngines.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ImageSearchEngines.all.filter { it.id != ImageSearchEngines.CHOOSER_ID }.all { it.url.startsWith("https://") })
    }

    @Test
    fun lensIsFirstAndSupportsShare() {
        val first = ImageSearchEngines.all.first()
        assertEquals(ImageSearchEngines.LENS_ID, first.id)
        assertTrue(first.supportsShare)
        assertTrue("com.google.ar.lens" in first.packages)
    }

    @Test
    fun appCapableEnginesAreLensYandexBingAndChooser() {
        assertEquals(
            listOf(ImageSearchEngines.LENS_ID, "yandex_images", "bing_visual", ImageSearchEngines.CHOOSER_ID),
            ImageSearchEngines.all.filter { it.supportsShare }.map { it.id }
        )
    }

    @Test
    fun chooserIsLastAndWebEnginesExist() {
        assertEquals(ImageSearchEngines.CHOOSER_ID, ImageSearchEngines.all.last().id)
        listOf("tineye", "google_images", "baidu_images", "saucenao").forEach { assertNotNull(ImageSearchEngines.byId(it)) }
        assertNull(ImageSearchEngines.byId("unknown"))
    }

    @Test
    fun descriptionsAreLocalised() {
        ImageSearchEngines.all.forEach { assertTrue(it.desc(true) != it.desc(false)) }
    }
}
