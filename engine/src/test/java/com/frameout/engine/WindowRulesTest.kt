package com.frameout.engine

import com.frameout.engine.player.WindowRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowRulesTest {
    private val start = 2_798L
    private val end = 5_321L

    @Test fun pausePastEndSettlesOnEnd() = assertEquals(end, WindowRules.settleAfterPause(start, end, end + 120))

    @Test fun pauseBeforeStartSettlesOnStart() = assertEquals(start, WindowRules.settleAfterPause(start, end, start - 90))

    @Test fun pauseInsideKeepsPosition() = assertNull(WindowRules.settleAfterPause(start, end, 4_000))

    @Test fun pauseFarOutsideKeepsPosition() {
        assertNull(WindowRules.settleAfterPause(start, end, 0))
        assertNull(WindowRules.settleAfterPause(start, end, end + 5_000))
    }

    @Test fun rangeChangeWithPlayheadAtZeroMovesToStart() = assertEquals(start, WindowRules.repairAfterRangeChange(start, end, 0))

    @Test fun rangeChangeWithPlayheadPastEndMovesToStart() = assertEquals(start, WindowRules.repairAfterRangeChange(start, end, end + 500))

    @Test fun rangeChangeKeepsPlayheadOnEdges() {
        assertNull(WindowRules.repairAfterRangeChange(start, end, start))
        assertNull(WindowRules.repairAfterRangeChange(start, end, end))
        assertNull(WindowRules.repairAfterRangeChange(start, end, start - 30))
        assertNull(WindowRules.repairAfterRangeChange(start, end, end + 30))
    }

    @Test fun playingOutsideIsDetected() {
        assertTrue(WindowRules.isOutside(start, end, end))
        assertTrue(WindowRules.isOutside(start, end, 0))
        assertFalse(WindowRules.isOutside(start, end, 4_000))
    }
}
