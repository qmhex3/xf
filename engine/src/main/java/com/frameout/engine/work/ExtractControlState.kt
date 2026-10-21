package com.frameout.engine.work

import com.frameout.core.db.ProjectDao
import com.frameout.core.model.ProjectStatus
import kotlinx.coroutines.CancellationException

enum class ControlState { Running, Paused, Cancelled }

object ControlResolver {
    fun resolve(memoryPaused: Boolean, memoryCancelled: Boolean, storedStatus: String?): ControlState = when {
        memoryCancelled || storedStatus == ProjectStatus.Cancelled.name -> ControlState.Cancelled
        memoryPaused || storedStatus == ProjectStatus.Paused.name -> ControlState.Paused
        else -> ControlState.Running
    }

    fun stoppedByUser(storedStatus: String?): Boolean =
        storedStatus == ProjectStatus.Paused.name || storedStatus == ProjectStatus.Cancelled.name
}

internal class ControlSync(private val dao: ProjectDao, private val projectId: Long, private val intervalMs: Long = 1_000L) {
    private var checkedAt = Long.MIN_VALUE
    private var stored: String? = null
    private var seen = false

    suspend fun state(now: Long = System.currentTimeMillis()): ControlState {
        if (checkedAt == Long.MIN_VALUE || now - checkedAt >= intervalMs) {
            stored = read()
            if (stored != null) seen = true
            checkedAt = now
        }
        if (seen && stored == null) return ControlState.Cancelled
        return ControlResolver.resolve(ExtractControl.isPaused(projectId), ExtractControl.isCancelled(projectId), stored)
    }

    private suspend fun read(): String? = try {
        dao.getById(projectId)?.status
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        stored
    }
}
