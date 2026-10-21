package com.frameout.app.extract

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.work.WorkInfo
import com.frameout.app.FrameOutApp
import com.frameout.core.db.progressTotal
import com.frameout.core.util.FrameStrings
import com.frameout.engine.work.ExtractScheduler
import com.frameout.engine.work.ExtractWorker

data class ExtractCapsuleState(
    val status: String = "",
    val done: Int = 0,
    val total: Int = 0,
    val elapsed: Long = 0L,
    val remaining: Long = 0L,
    val finished: Boolean = false,
    val message: String = "",
    val unified: Float = -1f
) {
    val estimated: Boolean get() = status == "Indexing"
    val paused: Boolean get() = status == "Paused"
    val shownTotal: Int get() = if (estimated) maxOf(total, done) else total
    val fraction: Float get() = when {
        status == "Completed" -> 1f
        unified >= 0f -> unified.coerceIn(0f, 1f)
        shownTotal > 0 -> (done.toFloat() / shownTotal).coerceIn(0f, 1f)
        else -> 0f
    }
    val percent: Int get() = (fraction * 100f).toInt()

    val counter: String get() = when {
        estimated -> ""
        shownTotal <= 0 -> ""
        else -> "$done/$shownTotal"
    }
}

object UnifiedProgress {
    const val INDEX_SHARE = 0.2f

    fun raw(status: String, done: Int, total: Int, indexingPhase: Boolean): Float {
        if (status == "Completed") return 1f
        if (indexingPhase) {
            val denominator = maxOf(total, done)
            return if (denominator > 0) INDEX_SHARE * (done.toFloat() / denominator).coerceIn(0f, 1f) else 0f
        }
        return if (total > 0) INDEX_SHARE + (1f - INDEX_SHARE) * (done.toFloat() / total).coerceIn(0f, 1f) else INDEX_SHARE
    }
}

internal class UnifiedProgressTracker {
    private var indexing: Boolean? = null
    private var peak = 0f

    fun update(state: ExtractCapsuleState, storedExtracted: Int): ExtractCapsuleState {
        when (state.status) {
            "Indexing" -> indexing = true
            "Extracting" -> indexing = false
        }
        val phaseIndexing = indexing ?: (storedExtracted <= 0)
        val raw = UnifiedProgress.raw(state.status, state.done, state.total, phaseIndexing)
        if (state.status == "Completed") peak = 1f else peak = maxOf(peak, raw)
        return state.copy(unified = peak)
    }
}

@Composable
fun rememberExtractCapsuleState(projectId: Long): ExtractCapsuleState {
    val context = LocalContext.current
    val info by ExtractScheduler.observe(context, projectId).collectAsState(initial = null)
    val dao = (context.applicationContext as FrameOutApp).container.projects
    val project by remember(projectId) { dao.observeById(projectId) }.collectAsState(initial = null)
    val stored = project?.let { storedState(it.status, it.extractedCount, it.progressTotal) }
    val tracker = remember(projectId) { UnifiedProgressTracker() }
    return tracker.update(resolveCapsule(liveState(info), stored), project?.extractedCount ?: 0)
}

fun resolveCapsule(live: ExtractCapsuleState, stored: ExtractCapsuleState?): ExtractCapsuleState = when {
    stored == null -> live
    live.finished -> stored
    else -> live.copy(
        status = if (isTransient(live.status) && stored.status in ACTIVE_STORED) stored.status else live.status,
        done = maxOf(live.done, stored.done),
        total = if (live.total > 0) live.total else stored.total
    )
}

private fun isTransient(status: String): Boolean = status.isEmpty() || status == "Queued" || status == "RUNNING" || status == "ENQUEUED" || status == "BLOCKED"

private val ACTIVE_STORED = setOf("Indexing", "Extracting", "Paused")

private fun liveState(info: WorkInfo?): ExtractCapsuleState {
    val progress = info?.progress
    return ExtractCapsuleState(
        status = progress?.getString(ExtractWorker.KEY_STATUS) ?: waitingStatus(info),
        done = progress?.getInt(ExtractWorker.KEY_DONE, 0) ?: 0,
        total = progress?.getInt(ExtractWorker.KEY_TOTAL, 0) ?: 0,
        elapsed = progress?.getLong(ExtractWorker.KEY_ELAPSED, 0L) ?: 0L,
        remaining = progress?.getLong(ExtractWorker.KEY_REMAINING, 0L) ?: 0L,
        finished = info?.state?.isFinished == true,
        message = progress?.getString(ExtractWorker.KEY_MESSAGE).orEmpty()
    )
}

private fun waitingStatus(info: WorkInfo?): String = when (info?.state) {
    WorkInfo.State.BLOCKED, WorkInfo.State.ENQUEUED -> "Queued"
    else -> info?.state?.name.orEmpty()
}

private fun storedState(status: String, done: Int, total: Int) = ExtractCapsuleState(
    status = status,
    done = done,
    total = total,
    finished = status == "Completed" || status == "Failed" || status == "Cancelled"
)

fun extractStatusLabel(value: String, message: String = ""): String = when (value) {
    "Indexing" -> FrameStrings.indexing()
    "Extracting" -> FrameStrings.extracting()
    "Completed" -> FrameStrings.completed()
    "Failed" -> {
        val human = com.frameout.engine.extract.ExtractFailureHandler.displayMessage(message)
        if (human.isNotBlank() && human != FrameStrings.failed()) human else FrameStrings.failed()
    }
    "Paused" -> if (message == com.frameout.engine.extract.ExtractProgress.MESSAGE_LOW_SPACE) FrameStrings.lowStorage() else FrameStrings.paused()
    "Cancelled" -> FrameStrings.cancelled()
    "Queued" -> FrameStrings.queued()
    else -> FrameStrings.working()
}

fun formatExtractTime(ms: Long): String = when {
    ms >= 3600000L -> FrameStrings.durationHm(ms / 3600000L, (ms / 60000L) % 60)
    ms >= 60000L -> FrameStrings.durationMs(ms / 60000L, (ms / 1000L) % 60)
    else -> FrameStrings.durationS(ms / 1000L)
}

