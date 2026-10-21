package com.frameout.engine.extract

import com.frameout.core.db.ProjectDao

internal class ProgressTally(candidates: Int, private val base: Int = 0) {
    @Volatile private var candidates = candidates
    private val clock = FlushClock()
    private var processed = 0

    fun advance() {
        processed++
    }

    fun retarget(value: Int) {
        candidates = value.coerceAtLeast(processed)
    }

    fun total(done: Int): Int {
        val remaining = (candidates - processed).coerceAtLeast(0)
        if (remaining == 0 || processed == 0) return done + remaining
        val saved = (done - base).coerceAtLeast(0)
        val projected = ((saved + 1L) * remaining + processed) / (processed + 1L)
        return done + projected.toInt()
    }

    suspend fun persist(dao: ProjectDao, projectId: Long, done: Int) {
        if (clock.due()) dao.setTargetCount(projectId, total(done))
    }
}
