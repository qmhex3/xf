package com.frameout.engine

import com.frameout.core.model.ProjectStatus
import com.frameout.engine.extract.ExtractFailureHandler
import com.frameout.engine.extract.LowSpaceException
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtractFailureStatusTest {
    @Test
    fun lowSpacePausesInsteadOfFailing() {
        assertEquals(ProjectStatus.Paused, ExtractFailureHandler.statusFor(LowSpaceException(), false))
    }

    @Test
    fun otherErrorsFail() {
        assertEquals(ProjectStatus.Failed, ExtractFailureHandler.statusFor(IllegalStateException("x"), false))
    }

    @Test
    fun cancellationWinsOverLowSpace() {
        assertEquals(ProjectStatus.Cancelled, ExtractFailureHandler.statusFor(LowSpaceException(), true))
    }
}
