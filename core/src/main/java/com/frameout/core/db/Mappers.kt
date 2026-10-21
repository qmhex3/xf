package com.frameout.core.db

import com.frameout.core.model.FrameItem
import com.frameout.core.model.Project
import com.frameout.core.model.ProjectStatus
import com.frameout.core.model.SavedImage

val ProjectEntity.progressTotal: Int get() = if (targetCount > 0) targetCount else frameCount

fun ProjectEntity.toModel(): Project {
    return Project(
        id = id,
        name = name,
        sourceUri = sourceUri,
        sourceName = sourceName,
        durationMs = durationMs,
        frameCount = frameCount,
        extractedCount = extractedCount,
        lastPtsUs = lastPtsUs,
        lastSampleIndex = lastSampleIndex,
        status = runCatching { ProjectStatus.valueOf(status) }.getOrDefault(ProjectStatus.Idle),
        createdAt = createdAt,
        updatedAt = updatedAt,
        thumbnailPath = thumbnailPath
    )
}

fun Project.toEntity(): ProjectEntity {
    return ProjectEntity(
        id = id,
        name = name,
        sourceUri = sourceUri,
        sourceName = sourceName,
        durationMs = durationMs,
        frameCount = frameCount,
        extractedCount = extractedCount,
        lastPtsUs = lastPtsUs,
        lastSampleIndex = lastSampleIndex,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        thumbnailPath = thumbnailPath
    )
}

fun FrameEntity.toModel(): FrameItem {
    return FrameItem(
        id = id,
        projectId = projectId,
        index = index,
        ptsUs = ptsUs,
        isKeyframe = isKeyframe,
        isFavorite = isFavorite,
        isDeleted = isDeleted,
        thumbnailPath = thumbnailPath,
        fullPath = fullPath
    )
}

fun SavedImageEntity.toModel(): SavedImage {
    return SavedImage(
        id = id,
        uri = uri,
        displayName = displayName,
        album = album,
        width = width,
        height = height,
        createdAt = createdAt,
        sourceFrameId = sourceFrameId
    )
}
