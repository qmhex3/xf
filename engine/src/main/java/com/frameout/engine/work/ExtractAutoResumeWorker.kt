package com.frameout.engine.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ExtractAutoResumeWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val projectId = inputData.getLong(KEY_PROJECT_ID, -1L)
        if (projectId < 0L) return Result.success()
        if (!FgsBudget.takeRotated(applicationContext, projectId)) return Result.success()
        ExtractScheduler.resume(applicationContext, projectId)
        return Result.success()
    }

    companion object {
        const val KEY_PROJECT_ID = "project_id"
        const val UNIQUE_PREFIX = "frameout_auto_resume_"
    }
}
