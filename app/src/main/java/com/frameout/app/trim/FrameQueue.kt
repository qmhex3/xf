package com.frameout.app.trim

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.frameout.core.util.FrameStrings

internal class QueueState {
    var items by mutableStateOf(emptyList<Long>())
        private set
    private var armed = false

    fun update(next: List<Long>) {
        items = next
        armed = false
    }

    fun back(leave: () -> Unit, warn: (String) -> Unit) {
        if (items.isEmpty() || armed) {
            leave()
        } else {
            armed = true
            warn(FrameStrings.queueUnsaved(items.size))
        }
    }
}

@Composable
internal fun rememberQueueState(): QueueState = remember { QueueState() }

@Composable
internal fun rememberGuardedBack(
    queue: QueueState, onBack: () -> Unit, onWarn: (CaptureStatusState) -> Unit, holding: Boolean = false, hold: () -> Boolean = { false }
): () -> Unit {
    val leave by rememberUpdatedState(onBack)
    val warn by rememberUpdatedState(onWarn)
    val held by rememberUpdatedState(hold)
    val guarded = { if (!held()) queue.back(leave) { warn(CaptureStatusState(it, error = true)) } }
    BackHandler(enabled = holding || queue.items.isNotEmpty(), onBack = guarded)
    return guarded
}

internal object FrameQueue {
    const val MAX = 200

    fun toggle(queue: List<Long>, positionMs: Long): List<Long> = when {
        positionMs in queue -> queue - positionMs
        queue.size >= MAX -> queue
        else -> (queue + positionMs).sorted()
    }

    fun contains(queue: List<Long>, positionMs: Long): Boolean = positionMs in queue
}
