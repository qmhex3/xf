package com.frameout.core.quality

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DetectorRunTest {
    private val hit = TagHit("l:cat", 0.9f)

    private fun detectors(total: Int, reported: MutableList<String> = ArrayList()) = DetectorRun(total) { name, _ -> reported += name }

    @Test
    fun healthyRunKeepsEveryHit() = runBlocking {
        val run = detectors(2)
        run.step("a") { listOf(hit) }
        run.step("b") { emptyList() }
        val outcome = run.outcome()
        assertEquals(listOf(hit), outcome.hits)
        assertTrue(outcome.failed.isEmpty())
        assertEquals(0, outcome.failedDetectors)
    }

    @Test
    fun oneFailureKeepsTheRestAndNamesTheDetector() = runBlocking {
        val reported = ArrayList<String>()
        val run = detectors(3, reported)
        run.step("a") { listOf(hit) }
        run.step("labels") { throw IllegalStateException("model missing") }
        run.step("c") { emptyList() }
        val outcome = run.outcome()
        assertEquals(listOf("labels"), outcome.failed)
        assertEquals(listOf(hit), outcome.hits)
        assertEquals(listOf("labels"), reported)
    }

    @Test
    fun everyDetectorFailingThrowsWithAllNames() {
        val run = detectors(2)
        try {
            runBlocking {
                run.step("a") { throw IllegalStateException("x") }
                run.step("b") { throw IllegalArgumentException("y") }
            }
            run.outcome()
            fail("expected AllDetectorsFailed")
        } catch (e: AllDetectorsFailed) {
            assertEquals(listOf("a", "b"), e.names)
        }
    }

    @Test
    fun cancellationIsNeverCountedAsAFailure() {
        val run = detectors(1)
        val thrown = runBlocking {
            try {
                run.step("a") { throw CancellationException("stop") }
                null
            } catch (e: CancellationException) {
                e
            }
        }
        assertNotNull(thrown)
        assertFalse(run.failed.contains("a"))
    }

    @Test
    fun refineRunsOnTheCollectedHits() = runBlocking {
        val run = detectors(1)
        run.step("a") { listOf(hit, TagHit("l:dog", 0.8f)) }
        val outcome = run.outcome { hits -> hits.filter { it.tag == "l:dog" } }
        assertEquals(listOf("l:dog"), outcome.hits.map { it.tag })
    }
}
