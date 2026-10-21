package com.frameout.app.trim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.launch

@Composable
internal fun CollectSection(env: TrimEnv) {
    val position by env.controller.position.collectAsState()
    var busy by remember { mutableStateOf(false) }
    val queued = FrameQueue.contains(env.queue.items, position)
    val count = env.queue.items.size
    val colors = FrameTheme.colors
    FrameText(TrimStrings.toolsCollect(), style = FrameTheme.typography.titleSmall)
    FrameText(TrimStrings.toolsCollectHint(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    val toggleLabel = if (queued) FrameStrings.queueRemove() else FrameStrings.queueAdd()
    TrimChip(toggleLabel, toggleLabel, Modifier.fillMaxWidth(), selected = queued, icon = if (queued) FrameIcons.Check else FrameIconsExtra.Plus) {
        env.queue.update(FrameQueue.toggle(env.queue.items, position))
    }
    if (count == 0) return
    FrameText(TrimStrings.toolsMarked(count), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        TrimChip(FrameStrings.queueSave(count), FrameStrings.queueSave(count), Modifier.weight(1f), selected = true, enabled = !busy, icon = FrameIcons.Save) {
            busy = true
            env.scope.launch {
                env.onStatus(saveQueuedFrames(env.controller, env.saver, env.prefs, env.uri, env.queue.items) { env.onStatus(it) })
                env.queue.update(emptyList())
                busy = false
            }
        }
        TrimChip("", FrameStrings.queueClear(), enabled = !busy, icon = FrameIcons.Close) { env.queue.update(emptyList()) }
    }
}
