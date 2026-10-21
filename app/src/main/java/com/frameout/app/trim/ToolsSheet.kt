package com.frameout.app.trim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.player.PlaybackSpeeds
import kotlinx.coroutines.launch

@Composable
internal fun BoxScope.ToolsSheet(env: TrimEnv, visible: Boolean) {
    val stored by env.prefs.jumpFrames.collectAsState()
    val frames = JumpSizes.normalize(stored)
    val speed = PlaybackSpeeds.label(env.player.speed)
    TrimSheetHost(visible, TrimStrings.toolsTitle(), { env.onSheet(TrimSheet.None) }) {
        ToolRow(TrimStrings.toolsSpeed(), speed, FrameStrings.playbackSpeed(speed), env.player.speed != PlaybackSpeeds.NORMAL, env.controller::cycleSpeed)
        ToolRow(TrimStrings.toolsJump(), FrameStrings.jumpChip(frames), FrameStrings.jumpSize(frames), false) { env.prefs.setJumpFrames(JumpSizes.next(frames)) }
        JumpButtons(env, frames)
        MarkerSection(env)
        CollectSection(env)
    }
}

@Composable
private fun ToolRow(title: String, value: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        FrameText(title, style = FrameTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        TrimChip(value, description, selected = selected, onClick = onClick)
    }
}

@Composable
private fun JumpButtons(env: TrimEnv, frames: Int) {
    val jump = { forward: Boolean -> env.scope.launch { env.controller.stepFrame(forward, frames) }; Unit }
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        TrimChip("\u2212$frames", FrameStrings.jumpBack(frames), Modifier.weight(1f)) { jump(false) }
        TrimChip("+$frames", FrameStrings.jumpForward(frames), Modifier.weight(1f)) { jump(true) }
    }
}

@Composable
private fun MarkerSection(env: TrimEnv) {
    val position by env.controller.position.collectAsState()
    MarkerJumpRow(env.markers, position, env.controller::seekTo)
}
