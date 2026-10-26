package com.frameout.engine.extract

import com.frameout.core.db.FrameEntity
import com.frameout.core.model.ProjectStatus

internal suspend fun ExtractRunner.persistInterrupted(request: ExtractRequest, state: LoopState, batch: List<FrameEntity>) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        logTiming(state)
        if (batch.isNotEmpty()) checkpointStore.commit(request.projectId, ProjectStatus.Paused, state.done, state.lastPts, state.lastSampleIndex, batch.toList())
        projectDao.updateCheckpoint(request.projectId, if (isCancelled()) ProjectStatus.Cancelled.name else ProjectStatus.Paused.name, state.done, state.lastPts.coerceAtLeast(0L), state.lastSampleIndex, System.currentTimeMillis())
    }
}

internal suspend fun ExtractRunner.persistFailed(request: ExtractRequest, state: LoopState, batch: List<FrameEntity>) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        runCatching {
            if (batch.isNotEmpty()) checkpointStore.commit(request.projectId, ProjectStatus.Extracting, state.done, state.lastPts, state.lastSampleIndex, batch.toList())
        }
    }
}

internal suspend fun ExtractRunner.finish(request: ExtractRequest, state: LoopState, batch: MutableList<FrameEntity>) {
    val status = if (isCancelled()) ProjectStatus.Cancelled else ProjectStatus.Completed
    timer.time(StageTimer.Stage.Commit) { checkpointStore.commit(request.projectId, status, state.done, state.lastPts, state.lastSampleIndex, batch.toList()) }
    logTiming(state)
    if (status == ProjectStatus.Completed) projectDao.setTargetCount(request.projectId, state.done)
}

internal suspend fun ExtractRunner.ensureRunning() = awaitRunnable(isCancelled, isPaused)
