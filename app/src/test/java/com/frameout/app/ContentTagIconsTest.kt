package com.frameout.app

import com.frameout.app.settings.contentTagIcon
import com.frameout.core.quality.content.ContentCatalog
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentTagIconsTest {
    @Test
    fun everyTagHasItsOwnIcon() {
        val fallback = contentTagIcon("no_such_tag")
        val missing = ContentCatalog.all.filter { it.id != "building" && contentTagIcon(it.id) == fallback }.map { it.id }
        assertTrue("tags without an icon: $missing", missing.isEmpty())
    }

    @Test
    fun iconsAreDistinct() {
        val ids = ContentCatalog.all.map { contentTagIcon(it.id) }
        assertTrue(ids.toSet().size == ids.size)
        assertNotEquals(0, ids.first())
    }
}
