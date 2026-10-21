package com.frameout.engine.work

import androidx.work.Data
import androidx.work.workDataOf
import com.frameout.engine.extract.ExtractFailureHandler
import com.frameout.engine.extract.ExtractProgress

internal class ProgressGate(private val minPushMs: Long) {
    private var lastStatus: ExtractProgress.Status? = null
    private var lastDone = -1
    private var lastTotal = -1
    private var lastPaused = false
    private var lastMessage = ""
    private var lastPushAt = 0L

    fun shouldPush(p: ExtractProgress, paused: Boolean, now: Long): Boolean {
        val statusChanged = p.status != lastStatus || paused != lastPaused || p.message != lastMessage
        val unchanged = !statusChanged && p.done == lastDone && p.total == lastTotal
        if (unchanged || (!statusChanged && now - lastPushAt < minPushMs)) return false
        lastStatus = p.status
        lastDone = p.done
        lastTotal = p.total
        lastPaused = paused
        lastMessage = p.message
        lastPushAt = now
        return true
    }
}

internal fun progressText(p: ExtractProgress): String = when {
    p.total > 0 -> "${p.done}/${p.total}"
    p.message.isBlank() -> ""
    else -> ExtractFailureHandler.displayMessage(p.message)
}

internal fun progressData(p: ExtractProgress, elapsed: Long, remaining: Long): Data = workDataOf(
    ExtractWorker.KEY_DONE to p.done,
    ExtractWorker.KEY_TOTAL to p.total,
    ExtractWorker.KEY_STATUS to p.status.name,
    ExtractWorker.KEY_ELAPSED to elapsed,
    ExtractWorker.KEY_REMAINING to remaining,
    ExtractWorker.KEY_MESSAGE to p.message
)
