package com.frameout.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LivePhotoMatcherRulesTest {
    private fun still(name: String, mime: String = "", bucket: String = "b1") = LivePhotoMatcher.StillRef(bucket, name, mime)

    @Test
    fun jpegPairsOnlyWithMov() {
        assertTrue(LivePhotoMatcher.canPair(stillIsHeif = false, videoExt = "mov"))
        assertTrue(LivePhotoMatcher.canPair(stillIsHeif = false, videoExt = "MOV"))
        assertFalse(LivePhotoMatcher.canPair(stillIsHeif = false, videoExt = "mp4"))
    }

    @Test
    fun heicKeepsTheOldMovOrMp4Rule() {
        assertTrue(LivePhotoMatcher.canPair(stillIsHeif = true, videoExt = "mov"))
        assertTrue(LivePhotoMatcher.canPair(stillIsHeif = true, videoExt = "mp4"))
        assertFalse(LivePhotoMatcher.canPair(stillIsHeif = true, videoExt = "mkv"))
    }

    @Test
    fun videoKeysCarryTheExtensionKind() {
        assertEquals("b1/img_001", LivePhotoMatcher.videoPairKey("b1", "IMG_001.MOV"))
        assertEquals("b1/img_001|mp4", LivePhotoMatcher.videoPairKey("b1", "IMG_001.mp4"))
        assertNull(LivePhotoMatcher.videoPairKey("b1", "IMG_001.mkv"))
        assertNull(LivePhotoMatcher.videoPairKey("b1", "IMG_001"))
    }

    @Test
    fun jpgWithSameNameMp4IsNotLive() {
        val keys = setOf(LivePhotoMatcher.videoPairKey("b1", "clip.mp4")!!)
        assertFalse(LivePhotoMatcher.isPairedStill(keys, "b1", "clip.jpg", "image/jpeg"))
        assertEquals(emptySet<String>(), LivePhotoMatcher.resolvePairs(listOf(still("clip.jpg", "image/jpeg")), keys))
    }

    @Test
    fun jpgWithSameNameMovIsLive() {
        val keys = setOf(LivePhotoMatcher.videoPairKey("b1", "IMG_0001.MOV")!!)
        assertTrue(LivePhotoMatcher.isPairedStill(keys, "b1", "IMG_0001.JPG", "image/jpeg"))
        assertEquals(setOf("b1/img_0001"), LivePhotoMatcher.resolvePairs(listOf(still("IMG_0001.JPG", "image/jpeg")), keys))
    }

    @Test
    fun heicWithSameNameMp4StaysLive() {
        val keys = setOf(LivePhotoMatcher.videoPairKey("b1", "photo.mp4")!!)
        assertTrue(LivePhotoMatcher.isPairedStill(keys, "b1", "photo.heic", "image/heic"))
        val resolved = LivePhotoMatcher.resolvePairs(listOf(still("photo.heic", "image/heic")), keys)
        assertEquals(setOf("b1/photo|mp4"), resolved)
        assertTrue(LivePhotoMatcher.isPairedStill(resolved, "b1", "photo.heic", "image/heic"))
        assertFalse(LivePhotoMatcher.isPairedStill(resolved, "b1", "photo.jpg", "image/jpeg"))
    }

    @Test
    fun differentFoldersNeverPair() {
        val keys = setOf(LivePhotoMatcher.videoPairKey("other", "IMG_1.MOV")!!)
        assertFalse(LivePhotoMatcher.isPairedStill(keys, "b1", "IMG_1.JPG", "image/jpeg"))
    }

    @Test
    fun stillClassificationIsUnchanged() {
        assertTrue(LivePhotoMatcher.isLiveStill("image/jpeg", "a.jpg"))
        assertTrue(LivePhotoMatcher.isLiveStill("", "a.HEIC"))
        assertFalse(LivePhotoMatcher.isLiveStill("image/png", "a.png"))
        assertTrue(LivePhotoMatcher.isHeif("image/heif", "noext"))
    }

    @Test
    fun heicWinsOverJpgWithTheSameNameAndMov() {
        val keys = setOf(LivePhotoMatcher.videoPairKey("b1", "a.mov")!!)
        val resolved = LivePhotoMatcher.resolvePairs(listOf(still("a.jpg", "image/jpeg"), still("a.heic", "image/heic")), keys)
        assertTrue(LivePhotoMatcher.isPairedStill(resolved, "b1", "a.heic", "image/heic"))
        assertFalse(LivePhotoMatcher.isPairedStill(resolved, "b1", "a.jpg", "image/jpeg"))
    }

    @Test
    fun jpgLosesToHeicFoundOutsideThePage() {
        val keys = setOf(LivePhotoMatcher.videoPairKey("b1", "a.mov")!!)
        val resolved = LivePhotoMatcher.resolvePairs(listOf(still("a.jpg", "image/jpeg")), keys, setOf("b1/a"))
        assertFalse(LivePhotoMatcher.isPairedStill(resolved, "b1", "a.jpg", "image/jpeg"))
    }

    @Test
    fun onlyHeicIsLiveWhenBothFormatsShareAnMp4() {
        val keys = setOf(LivePhotoMatcher.videoPairKey("b1", "a.mp4")!!)
        val resolved = LivePhotoMatcher.resolvePairs(listOf(still("a.jpg", "image/jpeg"), still("a.heic", "image/heic")), keys)
        assertTrue(LivePhotoMatcher.isPairedStill(resolved, "b1", "a.heic", "image/heic"))
        assertFalse(LivePhotoMatcher.isPairedStill(resolved, "b1", "a.jpg", "image/jpeg"))
    }

    @Test
    fun jpgStaysLiveWithoutAHeicSibling() {
        val keys = setOf(LivePhotoMatcher.videoPairKey("b1", "a.mov")!!)
        val resolved = LivePhotoMatcher.resolvePairs(listOf(still("a.jpg", "image/jpeg")), keys)
        assertTrue(LivePhotoMatcher.isPairedStill(resolved, "b1", "a.jpg", "image/jpeg"))
    }
}
