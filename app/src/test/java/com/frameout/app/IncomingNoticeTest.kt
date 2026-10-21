package com.frameout.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingNoticeTest {
    @Test
    fun copyFailureWinsOverRejection() {
        val result = IncomingMedia.Stabilized(emptyList(), failed = 1, rejected = 2)
        assertEquals(R.string.incoming_copy_failed, result.noticeRes())
    }

    @Test
    fun rejectionAloneGetsItsOwnNotice() {
        assertEquals(R.string.incoming_unsupported, IncomingMedia.Stabilized(emptyList(), failed = 0, rejected = 1).noticeRes())
    }

    @Test
    fun cleanResultHasNoNotice() {
        assertNull(IncomingMedia.Stabilized(emptyList(), failed = 0).noticeRes())
    }
}
