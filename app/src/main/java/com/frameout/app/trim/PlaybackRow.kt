package com.frameout.app.trim

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import com.frameout.designsystem.theme.frameControl
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameHoldRepeat
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.core.prefs.AppPrefs
import com.frameout.engine.player.PlayerController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.frameout.engine.player.PlaybackSpeeds

@Composable
fun JumpPlaybackRow(controller: PlayerController, speed: Float, prefs: AppPrefs, scope: CoroutineScope) {
    val stored by prefs.jumpFrames.collectAsState()
    val frames = JumpSizes.normalize(stored)
    PlaybackRow(speed, controller::cycleSpeed, frames, { prefs.setJumpFrames(JumpSizes.next(frames)) }) { forward ->
        scope.launch { controller.stepFrame(forward, frames) }
    }
}

@Composable
fun PlaybackRow(speed: Float, onSpeed: () -> Unit, frames: Int, onJumpSize: () -> Unit, onJump: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
        JumpChip("\u2212$frames", FrameStrings.jumpBack(frames)) { onJump(false) }
        JumpChip("+$frames", FrameStrings.jumpForward(frames)) { onJump(true) }
        SizeChip("\u00d7$frames", FrameStrings.jumpSize(frames), onJumpSize)
        Spacer(Modifier.weight(1f))
        SpeedChip(speed, onSpeed)
    }
}

@Composable
private fun SizeChip(label: String, description: String, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    FrameText(
        label,
        style = FrameTheme.typography.label,
        color = colors.primary,
        modifier = Modifier
            .frameControl(FrameShapes.pill, selected = true, glowAlpha = 0.5f)
            .clip(FrameShapes.pill)
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun JumpChip(label: String, description: String, onStep: () -> Unit) {
    val colors = FrameTheme.colors
    FrameText(
        label,
        style = FrameTheme.typography.label,
        color = colors.onSurface,
        modifier = Modifier
            .frameControl(FrameShapes.pill)
            .clip(FrameShapes.pill)
            .frameHoldRepeat(onStep)
            .semantics { contentDescription = description }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun SpeedChip(speed: Float, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val label = PlaybackSpeeds.label(speed)
    val active = speed != PlaybackSpeeds.NORMAL
    FrameText(
        label,
        style = FrameTheme.typography.label,
        color = if (active) Color.White else colors.onSurface,
        modifier = Modifier
            .frameControl(FrameShapes.pill, selected = active, solid = active, glowAlpha = if (active) 0.8f else 0f)
            .clip(FrameShapes.pill)
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = FrameStrings.playbackSpeed(label) }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
