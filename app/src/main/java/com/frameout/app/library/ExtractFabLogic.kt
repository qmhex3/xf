package com.frameout.app.library

import com.frameout.core.db.ProjectEntity
import com.frameout.core.db.progressTotal
import com.frameout.engine.work.UnifiedExtractProgress

enum class FabPhase { Add, Pause, Resume, Success }

data class FabProject(val id: Long, val status: String, val extracted: Int, val total: Int)

data class FabModel(val phase: FabPhase, val progress: Float? = null)

private val LIVE = setOf("Indexing", "Extracting")
private val RUNNING = LIVE + "Queued"
private val WITH_PROGRESS = setOf("Indexing", "Extracting", "Paused")

fun ProjectEntity.toFab() = FabProject(id, status, extractedCount, progressTotal)

fun pickFocus(projects: List<FabProject>, previous: Long?): Long? {
    val kept = projects.firstOrNull { it.id == previous }
    if (kept != null && kept.status in RUNNING) return kept.id
    projects.firstOrNull { it.status in LIVE }?.let { return it.id }
    projects.firstOrNull { it.status == "Queued" }?.let { return it.id }
    return if (kept?.status == "Paused") kept.id else null
}

fun justCompleted(projects: List<FabProject>, focus: Long?): Boolean =
    focus != null && projects.any { it.id == focus && it.status == "Completed" }

fun fabModel(projects: List<FabProject>, focus: Long?, flash: Boolean, live: Map<Long, Float> = emptyMap()): FabModel {
    if (flash) return FabModel(FabPhase.Success)
    val project = projects.firstOrNull { it.id == focus } ?: return FabModel(FabPhase.Add)
    val phase = if (project.status == "Paused") FabPhase.Resume else FabPhase.Pause
    val shown = if (project.status in WITH_PROGRESS) UnifiedExtractProgress.stored(project.status, project.extracted, project.total, live[project.id]) else null
    return FabModel(phase, shown)
}
