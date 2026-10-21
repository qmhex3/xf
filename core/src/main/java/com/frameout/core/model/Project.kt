package com.frameout.core.model

data class Project(
    val id: Long,
    val name: String,
    val sourceUri: String,
    val sourceName: String,
    val durationMs: Long,
    val frameCount: Int,
    val extractedCount: Int,
    val lastPtsUs: Long,
    val lastSampleIndex: Int = -1,
    val status: ProjectStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val thumbnailPath: String?
)

enum class ProjectStatus {
    Idle,
    Queued,
    Indexing,
    Extracting,
    Paused,
    Completed,
    Cancelled,
    Failed
}

data class FrameItem(
    val id: Long,
    val projectId: Long,
    val index: Int,
    val ptsUs: Long,
    val isKeyframe: Boolean,
    val isFavorite: Boolean,
    val isDeleted: Boolean,
    val thumbnailPath: String?,
    val fullPath: String?
)

data class SavedImage(
    val id: Long,
    val uri: String,
    val displayName: String,
    val album: String,
    val width: Int,
    val height: Int,
    val createdAt: Long,
    val sourceFrameId: Long?
)
