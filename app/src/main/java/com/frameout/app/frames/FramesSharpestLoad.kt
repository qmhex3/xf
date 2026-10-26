package com.frameout.app.frames

import android.app.Application
import com.frameout.core.quality.FrameSharpnessAnalyzer
import kotlinx.coroutines.flow.update

internal suspend fun FramesViewModel.loadSharpest() {
    val total = container.frames.countAll(projectId)
    if (!SharpestCache.isValid(getApplication<Application>(), projectId, total)) {
        _state.update { it.copy(analyzing = true) }
        val fresh = runCatching { FrameSharpnessAnalyzer.bestIds(container.frames, projectId) }
        fresh.onSuccess {
            container.sharpest.replace(projectId, it)
            SharpestCache.mark(getApplication<Application>(), projectId, total)
        }
        fresh.onFailure { container.sharpest.replace(projectId, emptyList()) }
    }
    _state.update { it.copy(sharpestVersion = it.sharpestVersion + 1, analyzing = false) }
    refresh(projectId)
}
