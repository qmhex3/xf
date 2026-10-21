package com.frameout.core.db

import com.frameout.core.model.Project
import com.frameout.core.model.ProjectStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(private val dao: ProjectDao) {
    fun observeAll(): Flow<List<Project>> {
        return dao.observeAll().map { list -> list.map { it.toModel() } }
    }

    suspend fun get(id: Long): Project? {
        return dao.getById(id)?.toModel()
    }

    suspend fun create(
        name: String,
        sourceUri: String,
        sourceName: String
    ): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            ProjectEntity(
                name = name,
                sourceUri = sourceUri,
                sourceName = sourceName,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateProgress(
        id: Long,
        status: ProjectStatus,
        extracted: Int,
        ptsUs: Long,
        sampleIndex: Int = -1
    ) {
        val now = System.currentTimeMillis()
        if (sampleIndex >= 0) dao.updateCheckpoint(id, status.name, extracted, ptsUs, sampleIndex, now)
        else dao.updateProgress(id, status.name, extracted, ptsUs, now)
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }
}
