package com.frameout.engine.work

import android.content.Context
import com.frameout.core.db.AppDatabase
import com.frameout.core.model.ProjectStatus
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal suspend fun markProjectPaused(context: Context, projectId: Long, notify: Boolean, text: String? = null) {
    withContext(NonCancellable) {
        val dao = AppDatabase.get(context).projectDao()
        if (dao.getById(projectId)?.status != ProjectStatus.Cancelled.name) {
            dao.setStatus(projectId, ProjectStatus.Paused.name, System.currentTimeMillis())
        }
    }
    if (notify) {
        if (text != null) ExtractNotifications.showPaused(context, projectId, text) else ExtractNotifications.showPaused(context, projectId)
    }
}
