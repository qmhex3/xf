package com.frameout.engine

import com.frameout.engine.extract.CandidateStepper
import com.frameout.engine.extract.ExtractMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CandidateStepperTest {
    @Test
    fun slidesKeepsOneFrameEveryQuarterSecond() {
        val stepper = CandidateStepper.forMode(ExtractMode.Slides)!!
        val kept = (0 until 90).filter { stepper.accept(it * 33_333L, false) }
        assertEquals(listOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 80, 88), kept)
    }

    @Test
    fun keyframesAlwaysPassAndRestartTheStep() {
        val stepper = CandidateStepper(1_000_000L)
        assertEquals(true, stepper.accept(0L, false))
        assertEquals(false, stepper.accept(100_000L, false))
        assertEquals(true, stepper.accept(200_000L, true))
        assertEquals(false, stepper.accept(900_000L, false))
        assertEquals(true, stepper.accept(1_200_000L, false))
    }

    @Test
    fun onlySceneAndSlidesModesStep() {
        assertNull(CandidateStepper.forMode(ExtractMode.EveryFrame))
        assertNull(CandidateStepper.forMode(ExtractMode.KeyframesOnly))
    }
}
