package com.frameout.engine.work

import android.content.Context
import com.frameout.core.db.AppDatabase
import com.frameout.core.model.ProjectStatus
import kotlinx.coroutines.CancellationException

internal suspend fun markFailedIfPending(context: Context, projectId: Long) {
    try {
        val dao = AppDatabase.get(context).projectDao()
        val status = dao.getById(projectId)?.status
        val pending = status == ProjectStatus.Queued.name ||
            status == ProjectStatus.Indexing.name ||
            status == ProjectStatus.Extracting.name ||
            status == ProjectStatus.Idle.name
        if (pending) dao.setStatus(projectId, ProjectStatus.Failed.name, System.currentTimeMillis())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
    }
}
