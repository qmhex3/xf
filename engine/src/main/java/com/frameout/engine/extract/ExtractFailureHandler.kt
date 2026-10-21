package com.frameout.engine.extract

import com.frameout.codecpack.MediaFailure
import com.frameout.codecpack.MediaFailureClassifier
import com.frameout.core.db.ProjectDao
import com.frameout.core.db.progressTotal
import com.frameout.core.model.ProjectStatus
import com.frameout.core.util.DiagLog
import com.frameout.core.util.FailureStrings
import com.frameout.core.util.FrameStrings
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

object ExtractFailureHandler {
    suspend fun handle(
        projectDao: ProjectDao,
        request: ExtractRequest,
        startTime: Long,
        error: Exception,
        cancelled: Boolean,
        emit: (Long, ExtractProgress.Status, Int, Int, Long, Long, String) -> Unit
    ) {
        val project = projectDao.getById(request.projectId)
        val lowSpace = !cancelled && error.isDiskFull()
        val status = statusFor(error, cancelled)
        val progress = when {
            cancelled -> ExtractProgress.Status.Cancelled
            lowSpace -> ExtractProgress.Status.Paused
            else -> ExtractProgress.Status.Failed
        }
        val message = wireMessage(error, cancelled)
        withContext(NonCancellable) {
            projectDao.updateProgress(request.projectId, status.name, project?.extractedCount ?: 0, project?.lastPtsUs ?: 0L, System.currentTimeMillis())
        }
        emit(request.projectId, progress, project?.extractedCount ?: 0, project?.progressTotal ?: 0, startTime, project?.lastPtsUs ?: 0L, message)
        if (!cancelled) DiagLog.append("extract", "${reasonOf(error).name} ${error.javaClass.simpleName}: ${error.message}")
    }

    fun statusFor(error: Exception, cancelled: Boolean): ProjectStatus = when {
        cancelled -> ProjectStatus.Cancelled
        error.isDiskFull() -> ProjectStatus.Paused
        else -> ProjectStatus.Failed
    }

    fun reasonOf(error: Throwable): MediaFailure = MediaFailureClassifier.classify(error)

    fun wireMessage(error: Exception, cancelled: Boolean): String {
        if (cancelled) return "Cancelled"
        if (error.isDiskFull()) return ExtractProgress.MESSAGE_LOW_SPACE
        return ExtractProgress.failureMessage(reasonOf(error))
    }

    fun humanMessage(error: Exception, cancelled: Boolean): String {
        if (cancelled) return FrameStrings.cancelExtract()
        if (error.isDiskFull()) return FrameStrings.lowStorage()
        return FailureStrings.of(reasonOf(error))
    }

    fun displayMessage(wire: String): String {
        ExtractProgress.failureOf(wire)?.let { return FailureStrings.of(it) }
        return when (wire) {
            ExtractProgress.MESSAGE_LOW_SPACE -> FrameStrings.lowStorage()
            ExtractProgress.MESSAGE_PAUSED_SYSTEM -> FrameStrings.paused()
            "Cancelled" -> FrameStrings.cancelExtract()
            "", "error" -> FrameStrings.failed()
            else -> wire
        }
    }
}
