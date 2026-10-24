package com.frameout.engine.extract

import org.junit.Assert.assertTrue
import org.junit.Test

class StageTimerTest {
    @Test
    fun summaryCountsEachStage() {
        val timer = StageTimer()
        timer.time(StageTimer.Stage.Write) { 1 }
        timer.time(StageTimer.Stage.Write) { 2 }
        timer.time(StageTimer.Stage.Decode) { 3 }
        val text = timer.summary(2, 2, 3)
        assertTrue(text, text.contains("write=") && text.contains("/2"))
        assertTrue(text, text.contains("decoders=2 writers=3"))
    }
}
