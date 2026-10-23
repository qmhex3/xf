package com.frameout.app.library

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.util.ExtractFabStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.dock.ChargeState
import com.frameout.designsystem.dock.DockItem
import com.frameout.designsystem.dock.FrameChargeFab
import com.frameout.designsystem.dock.FrameDock
import com.frameout.designsystem.dock.FrameFabCover
import com.frameout.designsystem.theme.FrameWindowSize
import com.frameout.designsystem.theme.frameWindowSize
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra

class LibrarySelectionDock(
    val actionId: String,
    val actionIcon: Int,
    val actionLabel: String,
    val onAction: () -> Unit,
    val onDelete: () -> Unit,
    val extra: List<DockItem> = emptyList()
)

class LibraryDockActions(
    val onAddVideo: () -> Unit,
    val onSettings: () -> Unit,
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onCancel: () -> Unit
)

@Composable
internal fun LibraryDock(
    state: LibraryState,
    actions: LibraryDockActions,
    modifier: Modifier = Modifier,
    selection: LibrarySelectionDock? = null,
    suggestion: VideoSuggestionUi? = null,
    suggestionVisible: Boolean = true,
    onSuggestion: () -> Unit = {},
    onSuggestionDismiss: () -> Unit = {}
) {
    val live by com.frameout.engine.work.LiveExtractProgress.all.collectAsState()
    val model = remember(state.projects, state.focusId, state.flashSuccess, live) {
        fabModel(state.projects.map { it.toFab() }, state.focusId, state.flashSuccess, live)
    }
    val items = if (selection != null) {
        listOf(
            DockItem(selection.actionId, selection.actionLabel, selection.actionIcon, onClick = selection.onAction)
        ) + selection.extra + listOf(
            DockItem("delete", FrameStrings.delete(), FrameIcons.Trash, destructive = true, onClick = selection.onDelete)
        )
    } else {
        listOf(
            DockItem("library", FrameStrings.library(), FrameIcons.Library, selectedIcon = FrameIcons.LibraryFilled, selected = true, onClick = {}),
            DockItem("settings", FrameStrings.settings(), FrameIcons.Settings, selectedIcon = FrameIcons.SettingsFilled, onClick = actions.onSettings)
        )
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        FrameDock(
            items = items,
            fab = if (selection == null) ({ ChargeButton(model, actions) }) else null,
            fabLabel = if (selection == null) dockFabLabel(model) else null
        )
        val compact = frameWindowSize(maxWidth - 32.dp) == FrameWindowSize.Compact
        if (selection == null && suggestion != null && compact && model.phase == FabPhase.Add) {
            FrameFabCover(
                cover = suggestion.cover,
                contentDescription = FrameStrings.latestVideoOpen(),
                dismissLabel = FrameStrings.latestVideoDismiss(),
                visible = suggestionVisible,
                onClick = onSuggestion,
                onDismiss = onSuggestionDismiss
            )
        }
    }
}

@Composable
private fun ChargeButton(model: FabModel, actions: LibraryDockActions, modifier: Modifier = Modifier) {
    val state = when (model.phase) {
        FabPhase.Add -> ChargeState.Idle
        FabPhase.Pause -> ChargeState.Running
        FabPhase.Resume -> ChargeState.Paused
        FabPhase.Success -> ChargeState.Success
    }
    val percent = model.progress?.let { ExtractFabStrings.percent((it * 100).toInt()) }
    FrameChargeFab(
        state = state,
        icon = fabIcon(model.phase),
        contentDescription = fabLabel(model.phase),
        onClick = fabAction(model.phase, actions),
        modifier = modifier,
        progress = model.progress,
        percentText = percent
    )
}

private fun dockFabLabel(model: FabModel): String {
    val base = fabLabel(model.phase)
    val working = model.phase == FabPhase.Pause || model.phase == FabPhase.Resume
    return model.progress?.takeIf { working }?.let { "$base ${ExtractFabStrings.percent((it * 100).toInt())}" } ?: base
}

private fun fabIcon(phase: FabPhase) = when (phase) {
    FabPhase.Add -> FrameIcons.VideoAdd
    FabPhase.Pause -> FrameIcons.Pause
    FabPhase.Resume -> FrameIconsExtra.Resume
    FabPhase.Success -> FrameIcons.Check
}

private fun fabLabel(phase: FabPhase) = when (phase) {
    FabPhase.Add -> FrameStrings.addVideo()
    FabPhase.Pause -> FrameStrings.pause()
    FabPhase.Resume -> FrameStrings.resume()
    FabPhase.Success -> FrameStrings.completed()
}

private fun fabAction(phase: FabPhase, actions: LibraryDockActions): () -> Unit = when (phase) {
    FabPhase.Add -> actions.onAddVideo
    FabPhase.Pause -> actions.onPause
    FabPhase.Resume -> actions.onResume
    FabPhase.Success -> ({})
}
