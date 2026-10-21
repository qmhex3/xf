package com.frameout.codecpack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class MediaFailureTest {
    @Test
    fun codesRoundTrip() {
        MediaFailure.entries.forEach { assertEquals(it, MediaFailure.fromCode(it.code)) }
    }

    @Test
    fun unknownCodeFallsBack() {
        assertEquals(MediaFailure.Unknown, MediaFailure.fromCode(9999))
    }

    @Test
    fun namesRoundTrip() {
        assertEquals(MediaFailure.Drm, MediaFailure.fromName("Drm"))
        assertNull(MediaFailure.fromName("missing"))
    }

    @Test
    fun nativeCodesAreStable() {
        assertEquals(5, MediaFailure.CorruptData.code)
        assertEquals(7, MediaFailure.DecoderMissing.code)
        assertEquals(9, MediaFailure.Drm.code)
        assertEquals(13, MediaFailure.UnsupportedContainer.code)
    }

    @Test
    fun fatalReasonsStopRetries() {
        assertTrue(MediaFailure.Drm.fatal)
        assertTrue(MediaFailure.DecoderMissing.fatal)
        assertFalse(MediaFailure.CorruptData.fatal)
        assertFalse(MediaFailure.IoError.fatal)
    }

    @Test
    fun classifierReadsExceptionTypes() {
        assertEquals(MediaFailure.SourceMissing, MediaFailureClassifier.classify(FileNotFoundException()))
        assertEquals(MediaFailure.PermissionDenied, MediaFailureClassifier.classify(SecurityException()))
        assertEquals(MediaFailure.IoError, MediaFailureClassifier.classify(IOException("x")))
        assertEquals(MediaFailure.OutOfMemory, MediaFailureClassifier.classify(OutOfMemoryError()))
        assertEquals(MediaFailure.Unknown, MediaFailureClassifier.classify(IllegalStateException("x")))
    }

    @Test
    fun classifierWalksCauses() {
        val wrapped = RuntimeException("outer", MediaFailureException(MediaFailure.Drm))
        assertEquals(MediaFailure.Drm, MediaFailureClassifier.classify(wrapped))
    }

    @Test
    fun exceptionKeepsReason() {
        assertEquals(MediaFailure.NoVideoStream, MediaFailureException(MediaFailure.NoVideoStream, "x").reason)
    }
}
