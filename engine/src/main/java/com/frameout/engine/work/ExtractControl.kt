package com.frameout.engine.work

import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

object ExtractControl {
    private val paused = ConcurrentHashMap.newKeySet<Long>()
    private val cancelled = ConcurrentHashMap.newKeySet<Long>()
    private val expectedStop = ConcurrentHashMap.newKeySet<Long>()
    private val generations = ConcurrentHashMap<Long, Long>()
    private val active = ConcurrentHashMap<Long, Any>()

    fun pause(projectId: Long) { paused.add(projectId) }
    fun resume(projectId: Long) { paused.remove(projectId); cancelled.remove(projectId); expectedStop.remove(projectId) }
    fun clearExpectedStop(projectId: Long) { expectedStop.remove(projectId) }
    fun expectStop(projectId: Long) { expectedStop.add(projectId) }
    fun wasExpectedStop(projectId: Long): Boolean = expectedStop.remove(projectId)

    fun cancel(projectId: Long) {
        cancelled.add(projectId)
        paused.remove(projectId)
    }

    fun release(projectId: Long) {
        paused.remove(projectId)
        cancelled.remove(projectId)
        expectedStop.remove(projectId)
    }

    fun releaseIfIdle(projectId: Long) {
        if (!active.containsKey(projectId)) release(projectId)
    }

    fun isPaused(projectId: Long): Boolean = paused.contains(projectId)
    fun isCancelled(projectId: Long): Boolean = cancelled.contains(projectId)

    fun clear(projectId: Long) {
        paused.remove(projectId)
        cancelled.remove(projectId)
    }

    fun nextGeneration(projectId: Long): Long = generations.merge(projectId, 1L) { a, b -> a + b } ?: 1L
    fun generation(projectId: Long): Long = generations[projectId] ?: 0L
    fun adoptGeneration(projectId: Long, generation: Long) { generations.merge(projectId, generation) { a, b -> maxOf(a, b) } }
    fun isCurrent(projectId: Long, generation: Long): Boolean = generation(projectId) == generation

    fun tryBegin(projectId: Long, owner: Any): Boolean = active.putIfAbsent(projectId, owner) == null
    fun end(projectId: Long, owner: Any) { active.remove(projectId, owner) }
    fun isActive(projectId: Long): Boolean = active.containsKey(projectId)

    suspend fun awaitIdle(projectId: Long, timeoutMs: Long, pollMs: Long = 50L): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (isActive(projectId)) {
            if (System.currentTimeMillis() >= deadline) return false
            delay(pollMs)
        }
        return true
    }
}
