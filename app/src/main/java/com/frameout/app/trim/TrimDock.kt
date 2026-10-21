package com.frameout.app.trim

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.frameout.core.storage.FrameCaptureSaver
import com.frameout.core.util.ClipStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameTopBar
import com.frameout.designsystem.dock.DockItem
import com.frameout.designsystem.dock.FrameDock
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.engine.player.PlayerController
import com.frameout.engine.player.PlayerUiState
import com.frameout.engine.timeline.TrimRange
import com.frameout.engine.timeline.formatTimestamp
import kotlinx.coroutines.launch

@Composable
internal fun TrimDock(
    controller: PlayerController, saver: FrameCaptureSaver, prefs: com.frameout.core.prefs.AppPrefs, uri: Uri, range: TrimRange, autoSave: Boolean,
    onToggleAutoSave: () -> Unit, onStatus: (CaptureStatusState) -> Unit, onExtract: (Uri, TrimRange, Boolean) -> Unit, onClip: () -> Unit, scope: kotlinx.coroutines.CoroutineScope
) {
    FrameDock(
        items = listOf(
            DockItem("autosave", FrameStrings.autoSave(), if (autoSave) FrameIcons.Check else FrameIconsExtra.Keyframe, selected = autoSave, onClick = onToggleAutoSave),
            DockItem("previous", FrameStrings.stepBack(), FrameIconsExtra.StepBack, repeat = true, onRepeat = { n -> scope.launch { controller.stepFrame(false, n) } }, onClick = { scope.launch { controller.stepFrame(false) } }),
            DockItem("next", FrameStrings.stepForward(), FrameIconsExtra.StepForward, repeat = true, onRepeat = { n -> scope.launch { controller.stepFrame(true, n) } }, onClick = { scope.launch { controller.stepFrame(true) } }),
            DockItem("clip", ClipStrings.saveClip(), FrameIconsExtra.Clip, onClick = onClip),
            DockItem("extract", FrameStrings.extract(), FrameIconsExtra.Live, onClick = { onExtract(uri, range, autoSave) })
        ),
        fab = {
            com.frameout.designsystem.dock.FrameFab(FrameIcons.Save, FrameStrings.captureFrame(), {
                scope.launch { onStatus(captureFrameToGallery(controller, saver, prefs, uri)) }
            })
        },
        compactLimit = 5,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
internal fun TrimTopBar(state: PlayerUiState, onBack: () -> Unit, controller: PlayerController) {
    val position by controller.position.collectAsState()
    FrameTopBar(
        title = formatTimestamp(position),
        onBack = onBack,
        backContentDescription = FrameStrings.back(),
        actionIcon = if (state.isPlaying) FrameIcons.Pause else FrameIcons.Play,
        actionLabel = if (state.isPlaying) FrameStrings.pause() else FrameStrings.play(),
        onAction = controller::toggle
    )
}
