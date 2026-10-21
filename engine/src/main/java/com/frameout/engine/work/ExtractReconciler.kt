package com.frameout.engine.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.frameout.core.db.AppDatabase
import com.frameout.core.model.ProjectStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExtractReconciler {
    private val IN_FLIGHT = listOf(ProjectStatus.Queued.name, ProjectStatus.Indexing.name, ProjectStatus.Extracting.name)

    suspend fun reconcile(context: Context): Int = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val dao = AppDatabase.get(app).projectDao()
        val wm = WorkManager.getInstance(app)
        val live = wm.getWorkInfosByTag(EXTRACT_TAG).get()
            .filter { !it.state.isFinished }
            .mapNotNull { info -> ExtractWorker.projectIdOf(info.tags) }
            .toSet()
        val orphans = dao.getByStatuses(IN_FLIGHT).filter { it.id !in live && !ExtractControl.isActive(it.id) }
        if (orphans.isEmpty()) return@withContext 0
        val requests = orphans.map(ExtractScheduler::requestOf)
        if (requests.size == 1) {
            ExtractScheduler.enqueue(app, requests.first())
        } else {
            ExtractScheduler.enqueueChain(wm, "${ExtractScheduler.BATCH_PREFIX}recovery_${System.currentTimeMillis()}", requests, ExistingWorkPolicy.KEEP)
        }
        requests.size
    }

    private const val EXTRACT_TAG = "extract"
}
