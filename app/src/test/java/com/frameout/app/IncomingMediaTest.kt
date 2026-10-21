package com.frameout.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IncomingMediaTest {
    @Test
    fun sanitizeKeepsSafeCharacters() {
        assertEquals("clip_1.mp4", IncomingMedia.sanitizeName("clip_1.mp4"))
    }

    @Test
    fun sanitizeReplacesUnsafeCharacters() {
        assertEquals("a_b_c.mp4", IncomingMedia.sanitizeName("a b/c.mp4"))
    }

    @Test
    fun sanitizeLimitsLengthAndHandlesBlank() {
        assertTrue(IncomingMedia.sanitizeName("x".repeat(300)).length <= 80)
        assertEquals("video", IncomingMedia.sanitizeName(""))
    }

    @Test
    fun foreignFileInsidePrivateDataIsRejected() {
        val data = File(System.getProperty("java.io.tmpdir"), "frameout_data_${System.nanoTime()}").apply { mkdirs() }
        val own = File(data, "files/incoming").apply { mkdirs() }
        val secret = File(data, "databases/x.db").apply { parentFile.mkdirs(); writeText("x") }
        val mine = File(own, "clip.mp4").apply { writeText("x") }
        val outside = File(System.getProperty("java.io.tmpdir"), "frameout_out_${System.nanoTime()}.mp4").apply { writeText("x") }
        assertTrue(IncomingMedia.isForeignPath(secret.path, listOf(data), own))
        assertFalse(IncomingMedia.isForeignPath(mine.path, listOf(data), own))
        assertFalse(IncomingMedia.isForeignPath(outside.path, listOf(data), own))
    }

    @Test
    fun mediaStoreUriWithFullAccessIsReadInPlace() {
        assertTrue(IncomingMedia.canReadWithoutCopy("media", 33, video = true, images = false, legacy = false))
        assertTrue(IncomingMedia.canReadWithoutCopy("media", 30, video = false, images = false, legacy = true))
    }

    @Test
    fun uriIsCopiedWhenAccessIsMissingOrNotMediaStore() {
        assertFalse(IncomingMedia.canReadWithoutCopy("media", 34, video = false, images = false, legacy = true))
        assertFalse(IncomingMedia.canReadWithoutCopy("media", 30, video = true, images = true, legacy = false))
        assertFalse(IncomingMedia.canReadWithoutCopy("com.other.provider", 33, video = true, images = true, legacy = true))
        assertFalse(IncomingMedia.canReadWithoutCopy(null, 33, video = true, images = true, legacy = true))
    }
}
