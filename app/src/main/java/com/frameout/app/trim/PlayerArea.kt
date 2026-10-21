package com.frameout.app.trim

import com.frameout.designsystem.theme.frameControl
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.player.PlayerController
import com.frameout.engine.player.PreviewNotice

@Composable
internal fun PlayerArea(controller: PlayerController, aspect: Float, modifier: Modifier, fx: SnapshotFxState? = null) {
    val notice = controller.state.collectAsState().value.notice
    val speedZones = rememberSpeedZoneState()
    Box(modifier.speedHold(controller, speedZones), Alignment.Center) {
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), Alignment.Center) {
            PlayerSurface(controller, aspect, Modifier.fillMaxSize(), onTap = controller::toggle, showMute = false) {
                if (fx != null) SnapshotOverlay(fx)
            }
            PreviewNoticeBadge(notice, Modifier.align(Alignment.BottomCenter))
        }
        SpeedZoneVisuals(speedZones)
    }
}

@Composable
private fun PreviewNoticeBadge(notice: PreviewNotice, modifier: Modifier) {
    val text = when (notice) {
        PreviewNotice.None -> return
        PreviewNotice.StillOnly -> FrameStrings.previewStillOnly()
        PreviewNotice.Unavailable -> FrameStrings.previewUnavailable()
    }
    FrameText(
        text,
        style = FrameTheme.typography.label,
        color = FrameTheme.colors.onSurface,
        modifier = modifier.padding(8.dp).frameControl(FrameShapes.pill, selected = true, glowAlpha = 0.4f).padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
