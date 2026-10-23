package com.frameout.engine.work
import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.frameout.core.db.AppDatabase
import com.frameout.core.db.ProjectEntity
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.storage.StorageManager
import com.frameout.core.util.FrameStrings
import com.frameout.engine.extract.GateFlags
import com.frameout.engine.extract.CheckpointStore
import com.frameout.engine.extract.ExtractEngine
import com.frameout.engine.extract.ExtractModeCodec
import com.frameout.engine.extract.ExtractProgress
import com.frameout.engine.extract.ExtractRequest
import com.frameout.engine.extract.SmartKeyframes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
class ExtractWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private var extractedThisRun = false
    private fun handleStopped() {
        val id = inputData.getLong(KEY_PROJECT_ID, -1L)
        if (id >= 0L && !isCurrentRun(id)) return
        val flagged = id >= 0L && ExtractControl.wasExpectedStop(id)
        val expected = flagged || (id >= 0L && ControlResolver.stoppedByUser(storedStatus(id)))
        val reason = if (Build.VERSION.SDK_INT >= 31) stopReason else WorkInfo.STOP_REASON_NOT_STOPPED
        if (id >= 0L && !expected && isSystemRecoveryStop(reason)) {
            AppPrefs(applicationContext).setRecoveryPending(false)
            ExtractNotifications.showPaused(applicationContext, id)
        } else if (id >= 0L && !expected && reason != WorkInfo.STOP_REASON_NOT_STOPPED) {
            AppPrefs(applicationContext).setRecoveryPending(true)
        }
    }
    private fun stoppedForRecovery(): Boolean = Build.VERSION.SDK_INT >= 31 && isSystemRecoveryStop(stopReason)
    private fun isCurrentRun(id: Long): Boolean = ExtractControl.isCurrent(id, inputData.getLong(KEY_GENERATION, 0L))
    private fun storedStatus(id: Long): String? =
        runCatching { AppDatabase.get(applicationContext).projectDao().getByIdSync(id)?.status }.getOrNull()
    private fun isSystemRecoveryStop(reason: Int): Boolean = reason == WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT || reason == WorkInfo.STOP_REASON_TIMEOUT
    override suspend fun getForegroundInfo(): ForegroundInfo =
        ExtractForeground.build(applicationContext, inputData.getLong(KEY_PROJECT_ID, -1L), FrameStrings.working(), 0, 0, true, false, 0L, 0L)
    private var foregroundWarned = false
    private suspend fun promote(info: ForegroundInfo) {
        try { setForeground(info) } catch (e: CancellationException) { throw e } catch (e: Exception) {
            com.frameout.core.util.DiagLog.append("extract", "foreground not allowed: ${e.javaClass.simpleName}: ${e.message}")
            foregroundWarned = ExtractForeground.denied(applicationContext, info, inputData.getLong(KEY_PROJECT_ID, -1L), foregroundWarned)
        }
    }
    override suspend fun doWork(): Result {
        val projectId = inputData.getLong(KEY_PROJECT_ID, -1L)
        if (projectId < 0L) return guarded(projectId)
        ExtractControl.adoptGeneration(projectId, inputData.getLong(KEY_GENERATION, 0L))
        val owner = Any()
        var begun = false
        return try {
            begun = ExtractControl.awaitIdle(projectId, BEGIN_WAIT_MS) && ExtractControl.tryBegin(projectId, owner)
            if (begun) ExtractNotifications.clearRetrying(applicationContext, projectId)
            if (!begun) {
                if (consumeRetry(projectId)) Result.retry() else Result.success()
            } else {
                guarded(projectId)
            }
        } finally {
            if (isStopped) handleStopped()
            if (begun) ExtractControl.end(projectId, owner)
            if (begun) LiveExtractProgress.clear(projectId)
            if (begun && !isStopped) announceFinish(projectId)
        }
    }
    private suspend fun announceFinish(projectId: Long) {
        withContext(Dispatchers.IO) {
            catching {
                val dao = AppDatabase.get(applicationContext).projectDao()
                val status = dao.getByIdSync(projectId)?.status
                val terminal = status == com.frameout.core.model.ProjectStatus.Completed.name ||
                    status == com.frameout.core.model.ProjectStatus.Failed.name ||
                    status == com.frameout.core.model.ProjectStatus.Cancelled.name
                if (!terminal) return@catching
                if (status == com.frameout.core.model.ProjectStatus.Completed.name && extractedThisRun) SmartKeyframes.refine(applicationContext, projectId)
                ExtractNotifications.cancel(applicationContext, projectId)
                if (status == com.frameout.core.model.ProjectStatus.Cancelled.name) return@catching
                val inBatch = tags.any { it.startsWith(ExtractScheduler.BATCH_PREFIX) }
                if (status == com.frameout.core.model.ProjectStatus.Completed.name && !inBatch) ExtractDoneNotification.show(applicationContext, projectId)
                val batchName = tags.firstOrNull { it.startsWith(ExtractScheduler.BATCH_PREFIX) } ?: return@catching
                val infos = androidx.work.WorkManager.getInstance(applicationContext).getWorkInfosForUniqueWork(batchName).get()
                if (infos.any { it.id != id && !it.state.isFinished }) return@catching
                val ids = infos.mapNotNull { projectIdOf(it.tags) }.distinct()
                if (ids.size < 2) return@catching
                val completed = ids.count { dao.getByIdSync(it)?.status == com.frameout.core.model.ProjectStatus.Completed.name }
                ExtractNotifications.showBatchComplete(applicationContext, batchName, completed, ids.size)
            }
        }
    }
    private suspend fun guarded(projectId: Long): Result = try {
        guardedRun(projectId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        com.frameout.core.util.DiagLog.append("extract", "worker setup failure: ${e.javaClass.simpleName}: ${e.message}")
        if (projectId >= 0L && isCurrentRun(projectId)) {
            recordFailure(projectId)
            ExtractControl.clear(projectId)
        }
        Result.success()
    }
    private suspend fun guardedRun(projectId: Long): Result {
        val startedAt = System.currentTimeMillis()
        if (inputData.getString(KEY_URI) == null || projectId < 0) {
            if (projectId >= 0) recordFailure(projectId)
            return Result.success()
        }
        val db = AppDatabase.get(applicationContext)
        val project = db.projectDao().getById(projectId) ?: return Result.success()
        val request = buildRequest(projectId, project, db)
        if (project.status == com.frameout.core.model.ProjectStatus.Cancelled.name || project.status == com.frameout.core.model.ProjectStatus.Paused.name) {
            if (isCurrentRun(projectId)) ExtractControl.clear(projectId)
            return Result.success()
        }
        if (project.status == com.frameout.core.model.ProjectStatus.Completed.name) return Result.success()
        ExtractControl.clearExpectedStop(projectId); com.frameout.engine.decode.DecodeQuality.reset()
        val engine = ExtractEngine(applicationContext, db.projectDao(), db.frameDao(), StorageManager(applicationContext), db.videoIndexDao(), CheckpointStore(db))
        val budget = if (Build.VERSION.SDK_INT >= 35) FgsBudget.tryAcquire(applicationContext, projectId) else null
        if (Build.VERSION.SDK_INT >= 35 && budget == null) {
            markRotationPaused(projectId)
            return Result.success()
        }
        return try {
            promote(ExtractForeground.build(applicationContext, projectId, FrameStrings.working(), 0, 0, true, false, 0L, 0L))
            val latest = db.projectDao().getById(projectId)?.status
            if (latest == com.frameout.core.model.ProjectStatus.Cancelled.name || latest == com.frameout.core.model.ProjectStatus.Paused.name) {
                if (isCurrentRun(projectId)) ExtractControl.clear(projectId)
                return Result.success()
            }
            extractedThisRun = true
            runExtraction(engine, request, startedAt, projectId)
        } finally {
            budget?.release(applicationContext)
        }
    }
    private suspend fun buildRequest(projectId: Long, project: ProjectEntity, db: AppDatabase): ExtractRequest {
        if (project.extractedCount == 0) {
            com.frameout.core.quality.SkipStats.reset(projectId)
            SmartKeyframes.reset(applicationContext, projectId)
        }
        var flags = project.gateFlags
        if (!GateFlags.isStored(flags)) {
            val prefs = AppPrefs(applicationContext)
            val smart = com.frameout.core.pro.ProAccess.permits(com.frameout.core.pro.ProFeature.SmartExtract)
            flags = GateFlags.encode(smart && prefs.skipBlurry.value, smart && prefs.skipDuplicates.value, smart && prefs.peopleOnly.value, prefs.peopleBody.value)
            db.projectDao().setGateFlags(projectId, flags)
        }
        return ExtractRequest(
            projectId,
            project.sourceUri,
            project.startMs,
            project.endMs,
            ExtractModeCodec.decode(project.extractMode),
            project.autoSave,
            inputData.getString(KEY_SAVE_FORMAT) ?: project.saveFormat,
            inputData.getInt(KEY_JPEG_QUALITY, project.jpegQuality).coerceIn(1, 100),
            skipBlurry = GateFlags.blurry(flags),
            skipDuplicates = GateFlags.duplicates(flags),
            peopleOnly = GateFlags.people(flags),
            peopleBody = GateFlags.body(flags)
        )
    }

    private suspend fun runExtraction(engine: ExtractEngine, request: ExtractRequest, startedAt: Long, projectId: Long): Result {
        var serviceRotation = false
        return try {
            coroutineScope {
                val job = launch { observeProgress(engine, projectId, startedAt) }
                val guard = launchFgsGuard(applicationContext, engine, projectId, FGS_BUDGET_POLL_MS) { serviceRotation = true }
                try {
                    engine.run(request)
                } finally {
                    job.cancel()
                    guard?.cancel()
                }
            }
            resultFor(engine, projectId, serviceRotation)
        } catch (e: CancellationException) {
            if (serviceRotation) return rotationResult(projectId)
            if (isCurrentRun(projectId) && !ExtractControl.isCancelled(projectId) && stoppedForRecovery()) markProjectPaused(applicationContext, projectId, notify = false)
            throw e
        } catch (e: Exception) {
            com.frameout.core.util.DiagLog.append("extract", "worker failure: ${e.javaClass.simpleName}: ${e.message}")
            val retryable = ExtractRetryBudget.isRetryable(com.frameout.engine.extract.ExtractFailureHandler.reasonOf(e))
            if (isCurrentRun(projectId) && retryable && consumeRetry(projectId)) {
                prepareRetry(projectId)
                Result.retry()
            } else {
                if (isCurrentRun(projectId)) recordFailure(projectId)
                Result.success()
            }
        } finally {
            if (isCurrentRun(projectId)) ExtractControl.clear(projectId)
        }
    }
    private suspend fun resultFor(engine: ExtractEngine, projectId: Long, serviceRotation: Boolean): Result {
        val progress = engine.progress.value
        val finished = progress.status == ExtractProgress.Status.Completed ||
            progress.status == ExtractProgress.Status.Cancelled ||
            progress.status == ExtractProgress.Status.Paused
        val current = isCurrentRun(projectId)
        if (finished && current) ExtractNotifications.showCompletionNotices(applicationContext, projectId, progress.message)
        if (serviceRotation) return rotationResult(projectId)
        if (finished) {
            if (current) ExtractRetryBudget.reset(applicationContext, projectId)
            return Result.success()
        }
        if (!current) return Result.success()
        val reason = ExtractProgress.failureOf(progress.message)
        if (ExtractRetryBudget.isRetryable(reason) && consumeRetry(projectId)) {
            prepareRetry(projectId)
            return Result.retry()
        }
        recordFailure(projectId)
        return Result.success()
    }
    private inline fun <T> catching(block: () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
    private fun consumeRetry(projectId: Long): Boolean =
        projectId >= 0L && ExtractRetryBudget.tryConsume(applicationContext, projectId, inputData.getLong(KEY_GENERATION, 0L))
    private suspend fun prepareRetry(projectId: Long) {
        catching {
            com.frameout.core.util.FailureMarker.clear(applicationContext, projectId)
            AppDatabase.get(applicationContext).projectDao()
                .setStatus(projectId, com.frameout.core.model.ProjectStatus.Queued.name, System.currentTimeMillis())
            ExtractNotifications.showRetrying(applicationContext, projectId)
        }
    }
    private suspend fun recordFailure(projectId: Long) = markFailedIfPending(applicationContext, projectId)
    private suspend fun rotationResult(projectId: Long): Result {
        markRotationPaused(projectId)
        return Result.success()
    }
    private suspend fun markRotationPaused(projectId: Long) {
        if (!isCurrentRun(projectId)) return
        val delayMs = FgsBudget.retryDelayMs(applicationContext)
        markProjectPaused(applicationContext, projectId, notify = true, text = FrameStrings.pausedBackgroundLimit(ExtractForeground.clockAfter(delayMs)))
        if (ExtractControl.isCancelled(projectId)) return
        FgsBudget.markRotated(applicationContext, projectId)
        ExtractScheduler.scheduleAutoResume(applicationContext, projectId, delayMs)
    }
    @OptIn(FlowPreview::class)
    private suspend fun observeProgress(engine: ExtractEngine, projectId: Long, startedAt: Long) {
        val estimator = RemainingEstimator()
        val sync = ControlSync(AppDatabase.get(applicationContext).projectDao(), projectId, CONTROL_SYNC_MS)
        val gate = ProgressGate(PUSH_MIN_MS)
        val meter = UnifiedProgressMeter()
        engine.progress.sample(PROGRESS_SAMPLE_MS).collect { p ->
            if (p.projectId != 0L && p.projectId != projectId) return@collect
            val control = sync.state()
            when (control) {
                ControlState.Cancelled -> engine.cancel()
                ControlState.Paused -> engine.pause()
                ControlState.Running -> engine.resume()
            }
            val paused = control == ControlState.Paused
            val now = System.currentTimeMillis()
            if (!gate.shouldPush(p, paused, now)) return@collect
            val elapsed = (now - startedAt).coerceAtLeast(0L)
            val remaining = estimator.update(p.status == ExtractProgress.Status.Extracting, paused, p.done, p.total, now)
            val title = extractStatusTitle(p.status)
            val indexing = p.status == ExtractProgress.Status.Indexing
            val text = if (indexing) "" else progressText(p)
            val unified = meter.update(p)
            LiveExtractProgress.publish(projectId, unified)
            val info = ExtractForeground.build(
                applicationContext, projectId, if (text.isBlank()) title else "$title · $text", p.done, p.total, p.total <= 0, paused, elapsed, remaining, unified, indexing
            )
            promote(info)
            setProgress(progressData(p, elapsed, remaining))
        }
    }
    companion object {
        const val KEY_PROJECT_ID = "project_id"; const val KEY_URI = "uri"
        const val KEY_SAVE_FORMAT = "save_format"
        const val KEY_JPEG_QUALITY = "jpeg_quality"
        const val KEY_DONE = "done"; const val KEY_TOTAL = "total"; const val KEY_STATUS = "status"
        const val KEY_GENERATION = "generation"
        const val KEY_ELAPSED = "elapsed"; const val KEY_REMAINING = "remaining"; const val KEY_MESSAGE = "message"
        const val UNIQUE_PREFIX = "extract_"
        const val PROJECT_TAG_PREFIX = "frameout_project_"
        fun projectTag(projectId: Long): String = "$PROJECT_TAG_PREFIX$projectId"
        fun projectIdOf(tags: Set<String>): Long? =
            tags.firstNotNullOfOrNull { if (it.startsWith(PROJECT_TAG_PREFIX)) it.removePrefix(PROJECT_TAG_PREFIX).toLongOrNull() else null }
                ?: tags.firstNotNullOfOrNull { if (it.startsWith(UNIQUE_PREFIX)) it.removePrefix(UNIQUE_PREFIX).toLongOrNull() else null }
        private const val BEGIN_WAIT_MS = 10L * 1000L
        private const val FGS_BUDGET_POLL_MS = 15L * 1000L
        private const val PROGRESS_SAMPLE_MS = 1000L
        private const val PUSH_MIN_MS = 2000L
        private const val CONTROL_SYNC_MS = 3000L
    }
}
