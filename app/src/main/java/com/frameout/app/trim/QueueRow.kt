package com.frameout.app.trim

import androidx.compose.ui.draw.clip
import com.frameout.designsystem.theme.frameControl
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.storage.FrameCaptureSaver
import com.frameout.engine.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill

@Composable
internal fun CaptureQueueRow(
    queueState: QueueState, controller: PlayerController, saver: FrameCaptureSaver, prefs: AppPrefs, uri: Uri,
    positionMs: Long, scope: CoroutineScope, onStatus: (CaptureStatusState) -> Unit
) {
    val queue = queueState.items
    var busy by remember { mutableStateOf(false) }
    val save = {
        busy = true
        scope.launch {
            onStatus(saveQueuedFrames(controller, saver, prefs, uri, queue, onStatus))
            queueState.update(emptyList())
            busy = false
        }
        Unit
    }
    QueueRow(queue, positionMs, busy, { queueState.update(FrameQueue.toggle(queue, positionMs)) }, save, { queueState.update(emptyList()) })
}

@Composable
private fun QueueRow(queue: List<Long>, positionMs: Long, busy: Boolean, onToggle: () -> Unit, onSave: () -> Unit, onClear: () -> Unit) {
    val queued = FrameQueue.contains(queue, positionMs)
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
        QueueChip(if (queued) "✓ ${queue.size}" else "+ ${queue.size}", if (queued) FrameStrings.queueRemove() else FrameStrings.queueAdd(), queued, !busy, onToggle)
        if (queue.isNotEmpty()) {
            QueueChip(FrameStrings.queueSave(queue.size), FrameStrings.queueSave(queue.size), false, !busy, onSave)
            QueueChip("✕", FrameStrings.queueClear(), false, !busy, onClear)
        }
    }
}

@Composable
private fun QueueChip(label: String, description: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    FrameText(
        label,
        style = FrameTheme.typography.label,
        color = if (active) colors.onPrimary else colors.onSurface,
        modifier = Modifier
            .frameControl(FrameShapes.pill, selected = active, solid = active, glowAlpha = if (active) 0.7f else 0f)
            .clip(FrameShapes.pill)
            .frameClickable(onClick = { if (enabled) onClick() })
            .semantics { contentDescription = description }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
