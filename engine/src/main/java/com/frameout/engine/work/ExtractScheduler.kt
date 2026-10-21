package com.frameout.engine.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.frameout.engine.extract.ExtractModeCodec
import com.frameout.engine.extract.ExtractRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object ExtractScheduler {
    private val failures = kotlinx.coroutines.CoroutineExceptionHandler { _, error ->
        com.frameout.core.util.DiagLog.append("extract", "scheduler failure: ${error.javaClass.simpleName}: ${error.message}")
    }
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO + failures)
    const val BATCH_PREFIX = "frameout_batch_"
    private const val IDLE_GRACE_MS = 8000L
    private val CANCELLABLE = setOf(
        com.frameout.core.model.ProjectStatus.Idle.name,
        com.frameout.core.model.ProjectStatus.Queued.name,
        com.frameout.core.model.ProjectStatus.Indexing.name,
        com.frameout.core.model.ProjectStatus.Extracting.name,
        com.frameout.core.model.ProjectStatus.Paused.name
    )

    fun enqueue(context: Context, request: ExtractRequest, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP, generation: Long = ExtractControl.generation(request.projectId)) {
        WorkManager.getInstance(context).enqueueUniqueWork("${ExtractWorker.UNIQUE_PREFIX}${request.projectId}", policy, buildRequest(request, generation = generation))
    }

    fun enqueueBatch(context: Context, requests: List<ExtractRequest>, batchId: String) {
        if (requests.isEmpty()) return
        if (requests.size == 1) { enqueue(context, requests.first()); return }
        enqueueChain(WorkManager.getInstance(context), "$BATCH_PREFIX$batchId", requests, ExistingWorkPolicy.KEEP)
    }

    internal fun enqueueChain(wm: WorkManager, name: String, requests: List<ExtractRequest>, policy: ExistingWorkPolicy) {
        if (requests.isEmpty()) return
        var chain = wm.beginUniqueWork(name, policy, buildRequest(requests.first(), name))
        for (i in 1 until requests.size) chain = chain.then(buildRequest(requests[i], name))
        chain.enqueue()
    }

    internal fun requestOf(project: com.frameout.core.db.ProjectEntity): ExtractRequest =
        ExtractRequest(project.id, project.sourceUri, project.startMs, project.endMs, ExtractModeCodec.decode(project.extractMode), project.autoSave, project.saveFormat, project.jpegQuality)

    fun scheduleAutoResume(context: Context, projectId: Long, delayMs: Long) {
        val request = androidx.work.OneTimeWorkRequestBuilder<ExtractAutoResumeWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ExtractAutoResumeWorker.KEY_PROJECT_ID to projectId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("${ExtractAutoResumeWorker.UNIQUE_PREFIX}$projectId", ExistingWorkPolicy.REPLACE, request)
    }

    private fun dropAutoResume(context: Context, projectId: Long) {
        FgsBudget.clearRotated(context, projectId)
        WorkManager.getInstance(context).cancelUniqueWork("${ExtractAutoResumeWorker.UNIQUE_PREFIX}$projectId")
    }

    fun pause(context: Context, projectId: Long): kotlinx.coroutines.Job = io.launch {
        dropAutoResume(context, projectId)
        val db = com.frameout.core.db.AppDatabase.get(context)
        if (db.projectDao().getById(projectId)?.status != com.frameout.core.model.ProjectStatus.Completed.name) {
            db.projectDao().setStatus(projectId, com.frameout.core.model.ProjectStatus.Paused.name, System.currentTimeMillis())
        }
        ExtractControl.pause(projectId)
        if (ExtractControl.isActive(projectId)) ExtractControl.expectStop(projectId)
        stopProjectWork(context, projectId)
    }

    fun resume(context: Context, projectId: Long): kotlinx.coroutines.Job = io.launch {
        dropAutoResume(context, projectId)
        if (ExtractControl.isActive(projectId)) ExtractControl.expectStop(projectId)
        stopProjectWork(context, projectId)
        val db = com.frameout.core.db.AppDatabase.get(context)
        val project = db.projectDao().getById(projectId) ?: return@launch
        if (project.status == com.frameout.core.model.ProjectStatus.Cancelled.name) return@launch
        val generation = ExtractControl.nextGeneration(projectId)
        db.projectDao().setStatus(projectId, com.frameout.core.model.ProjectStatus.Extracting.name, System.currentTimeMillis())
        ExtractControl.resume(projectId)
        enqueue(context, requestOf(project), ExistingWorkPolicy.REPLACE, generation)
    }

    fun cancel(context: Context, projectId: Long): kotlinx.coroutines.Job {
        dropAutoResume(context, projectId)
        ExtractControl.cancel(projectId)
        if (ExtractControl.isActive(projectId)) ExtractControl.expectStop(projectId)
        ExtractNotifications.cancel(context, projectId)
        return io.launch {
            markCancelled(context, projectId)
            stopProjectWork(context, projectId)
            markCancelled(context, projectId)
            ExtractControl.awaitIdle(projectId, 5000L)
            ExtractControl.releaseIfIdle(projectId)
        }
    }

    suspend fun cancelAndAwait(context: Context, projectId: Long, timeoutMs: Long = 5000L): Boolean {
        dropAutoResume(context, projectId)
        ExtractControl.cancel(projectId)
        if (ExtractControl.isActive(projectId)) ExtractControl.expectStop(projectId)
        markCancelled(context, projectId)
        stopProjectWork(context, projectId, timeoutMs)
        ExtractNotifications.cancel(context, projectId)
        markCancelled(context, projectId)
        val idle = ExtractControl.awaitIdle(projectId, IDLE_GRACE_MS)
        if (idle) ExtractControl.releaseIfIdle(projectId)
        return idle
    }

    fun cancelBatch(context: Context, batchId: String) {
        val wm = WorkManager.getInstance(context)
        io.launch {
            val dao = com.frameout.core.db.AppDatabase.get(context).projectDao()
            val ids = wm.getWorkInfosForUniqueWork("$BATCH_PREFIX$batchId").get()
                .filter { !it.state.isFinished }
                .mapNotNull { projectIdOf(it) }
            ids.forEach { id ->
                val status = dao.getById(id)?.status
                if (status != null && status in CANCELLABLE) {
                    dropAutoResume(context, id)
                    ExtractControl.cancel(id)
                    if (ExtractControl.isActive(id)) ExtractControl.expectStop(id)
                    ExtractNotifications.cancel(context, id)
                    dao.setStatus(id, com.frameout.core.model.ProjectStatus.Cancelled.name, System.currentTimeMillis())
                }
            }
            wm.cancelUniqueWork("$BATCH_PREFIX$batchId")
            ids.forEach { id ->
                ExtractControl.awaitIdle(id, 5000L)
                ExtractControl.releaseIfIdle(id)
            }
        }
    }

    fun observe(context: Context, projectId: Long): Flow<WorkInfo?> = WorkManager.getInstance(context).getWorkInfosByTagFlow("${ExtractWorker.UNIQUE_PREFIX}$projectId").map { infos ->
        infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            ?: infos.firstOrNull { !it.state.isFinished }
            ?: infos.lastOrNull()
    }

    fun observeBatch(context: Context, batchId: String): Flow<List<WorkInfo>> = WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow("$BATCH_PREFIX$batchId")

    private fun projectIdOf(info: WorkInfo): Long? =
        ExtractWorker.projectIdOf(info.tags)

    private fun batchNameOf(info: WorkInfo): String? = info.tags.firstOrNull { it.startsWith(BATCH_PREFIX) }

    private fun buildRequest(request: ExtractRequest, batchName: String? = null, generation: Long = ExtractControl.generation(request.projectId)): OneTimeWorkRequest {
        val data = workDataOf(
            ExtractWorker.KEY_PROJECT_ID to request.projectId,
            ExtractWorker.KEY_URI to request.uri,
            ExtractWorker.KEY_SAVE_FORMAT to request.saveFormat,
            ExtractWorker.KEY_JPEG_QUALITY to request.jpegQuality,
            ExtractWorker.KEY_GENERATION to generation
        )
        val builder = OneTimeWorkRequestBuilder<ExtractWorker>().setInputData(data).setBackoffCriteria(BackoffPolicy.LINEAR, 10L, TimeUnit.SECONDS).addTag("extract").addTag("${ExtractWorker.UNIQUE_PREFIX}${request.projectId}").addTag(ExtractWorker.projectTag(request.projectId))
        if (batchName != null) builder.addTag(batchName) else builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        return builder.build()
    }

    private suspend fun markCancelled(context: Context, projectId: Long) {
        val dao = com.frameout.core.db.AppDatabase.get(context).projectDao()
        if (dao.getById(projectId)?.status == com.frameout.core.model.ProjectStatus.Completed.name) return
        dao.setStatus(projectId, com.frameout.core.model.ProjectStatus.Cancelled.name, System.currentTimeMillis())
    }

    private suspend fun stopProjectWork(context: Context, projectId: Long, timeoutMs: Long = 5000L) {
        val wm = WorkManager.getInstance(context)
        val tag = "${ExtractWorker.UNIQUE_PREFIX}$projectId"
        val deadline = System.currentTimeMillis() + timeoutMs
        val unfinished = withContext(Dispatchers.IO) { wm.getWorkInfosByTag(tag).get() }.filter { !it.state.isFinished }
        val inBatch = unfinished.filter { batchNameOf(it) != null }
        val standalone = unfinished - inBatch.toSet()
        val running = inBatch.filter { it.state == WorkInfo.State.RUNNING }
        val orphaned = running.mapNotNull { batchNameOf(it) }.distinct().associateWith { name ->
            withContext(Dispatchers.IO) { wm.getWorkInfosForUniqueWork(name).get() }
                .filter { !it.state.isFinished && it.state != WorkInfo.State.RUNNING }
                .mapNotNull { projectIdOf(it) }
                .filter { it != projectId }
        }
        wm.cancelUniqueWork(tag)
        (standalone + running).forEach { wm.cancelWorkById(it.id) }
        val awaited = (standalone + running).map { it.id }
        withContext(Dispatchers.IO) {
            while (System.currentTimeMillis() < deadline) {
                val settled = awaited.all { id -> wm.getWorkInfoById(id).get()?.state?.isFinished != false }
                if (settled && !ExtractControl.isActive(projectId)) return@withContext
                delay(100)
            }
        }
        orphaned.forEach { (name, ids) -> rebuildBatch(context, wm, name, ids) }
    }

    private suspend fun rebuildBatch(context: Context, wm: WorkManager, name: String, ids: List<Long>) {
        val dao = com.frameout.core.db.AppDatabase.get(context).projectDao()
        val requests = ids.distinct().sorted().mapNotNull { id ->
            dao.getById(id)?.takeIf { it.status == com.frameout.core.model.ProjectStatus.Queued.name && !ExtractControl.isCancelled(id) }?.let(::requestOf)
        }
        if (requests.isEmpty()) return
        enqueueChain(wm, name, requests, ExistingWorkPolicy.REPLACE)
    }
}
