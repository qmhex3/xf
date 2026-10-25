package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractQualityGateBlockTest {
    private val size = 64

    private fun sharp(phase: Int) = IntArray(size * size) { i -> if (((i % size) / 2 + (i / size) / 2 + phase) % 2 == 0) 20 else 230 }

    private fun warmed(gate: ExtractQualityGate, factor: Double) = repeat(10) { assertTrue(gate.accept(sharp(it % 2), size, size, factor)) }

    @Test
    fun lowFactorTurnsASharpLookingFrameIntoABlurryOne() {
        val gate = ExtractQualityGate(true, false)
        warmed(gate, 1.0)
        assertEquals(false, gate.accept(sharp(0), size, size, 0.2))
        assertTrue(gate.accept(sharp(0), size, size))
    }

    @Test
    fun aClipThatIsUniformlyBlockyIsNotRejectedAsAWhole() {
        val gate = ExtractQualityGate(true, false)
        warmed(gate, 0.3)
        assertTrue(gate.accept(sharp(0), size, size, 0.3))
    }

    @Test
    fun seededFactorsPutTheBaselineOnTheCorrectedScale() {
        val gate = ExtractQualityGate(true, false)
        repeat(10) { gate.seed(sharp(it % 2), size, size, 0.5) }
        assertTrue(gate.accept(sharp(0), size, size, 0.5))
        assertEquals(false, gate.accept(sharp(0), size, size, 0.05))
    }

    @Test
    fun factorIsIgnoredWhenTheBlurFilterIsOff() {
        val gate = ExtractQualityGate(false, false)
        warmed(gate, 1.0)
        assertTrue(gate.accept(sharp(0), size, size, 0.01))
    }
}
