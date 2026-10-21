package com.frameout.engine

import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureException
import com.frameout.engine.extract.ExtractFailureHandler
import com.frameout.engine.extract.ExtractProgress
import com.frameout.engine.extract.FrameDecodeException
import com.frameout.engine.extract.LowSpaceException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtractFailureWireTest {
    @Test
    fun reasonTravelsAsWireMessage() {
        val wire = ExtractFailureHandler.wireMessage(MediaFailureException(MediaFailure.Drm), false)
        assertEquals("failure:Drm", wire)
        assertEquals(MediaFailure.Drm, ExtractProgress.failureOf(wire))
    }

    @Test
    fun frameDecodeKeepsItsReason() {
        val error = FrameDecodeException(3, "x", MediaFailure.DecoderMissing)
        assertEquals(MediaFailure.DecoderMissing, ExtractFailureHandler.reasonOf(error))
    }

    @Test
    fun frameDecodeDefaultsToCorrupt() {
        assertEquals(MediaFailure.CorruptData, ExtractFailureHandler.reasonOf(FrameDecodeException(0, "x")))
    }

    @Test
    fun lowSpaceKeepsItsOwnMessage() {
        assertEquals(ExtractProgress.MESSAGE_LOW_SPACE, ExtractFailureHandler.wireMessage(LowSpaceException(), false))
    }

    @Test
    fun cancellationKeepsItsOwnMessage() {
        assertEquals("Cancelled", ExtractFailureHandler.wireMessage(MediaFailureException(MediaFailure.Drm), true))
    }

    @Test
    fun plainMessagesAreNotReasons() {
        assertNull(ExtractProgress.failureOf("low_space"))
        assertNull(ExtractProgress.failureOf("failure:NotAReason"))
    }
}
