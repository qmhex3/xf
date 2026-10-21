package com.frameout.engine

import com.frameout.engine.player.StepQueue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StepQueueTest {
    @Test
    fun appliesSingleStepImmediately() = runTest {
        val applied = mutableListOf<Int>()
        StepQueue { applied.add(it) }.push(3)
        assertEquals(listOf(3), applied)
    }

    @Test
    fun coalescesPushesArrivingWhileBusy() = runTest {
        val applied = mutableListOf<Int>()
        val gate = CompletableDeferred<Unit>()
        val queue = StepQueue { delta ->
            applied.add(delta)
            if (applied.size == 1) gate.await()
        }
        launch { queue.push(1) }
        runCurrent()
        queue.push(1)
        queue.push(1)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf(1, 2), applied)
    }

    @Test
    fun oppositeDirectionsCancelOut() = runTest {
        val applied = mutableListOf<Int>()
        val gate = CompletableDeferred<Unit>()
        val queue = StepQueue { delta ->
            applied.add(delta)
            if (applied.size == 1) gate.await()
        }
        launch { queue.push(1) }
        runCurrent()
        queue.push(-3)
        queue.push(1)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf(1, -2), applied)
    }
}
