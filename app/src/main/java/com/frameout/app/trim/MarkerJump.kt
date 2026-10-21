package com.frameout.app.trim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.engine.audio.MarkerNav

@Composable
fun MarkerJumpRow(markers: List<Long>, positionMs: Long, onSeek: (Long) -> Unit) {
    if (markers.isEmpty()) return
    val previous = MarkerNav.previous(markers, positionMs)
    val next = MarkerNav.next(markers, positionMs)
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        TrimChip(FrameStrings.audioMarkerPrev(), FrameStrings.audioMarkerPrev(), Modifier.weight(1f), enabled = previous != null) { previous?.let(onSeek) }
        TrimChip(FrameStrings.audioMarkerNext(), FrameStrings.audioMarkerNext(), Modifier.weight(1f), enabled = next != null) { next?.let(onSeek) }
    }
}
