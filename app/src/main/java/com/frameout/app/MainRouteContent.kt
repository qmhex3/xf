package com.frameout.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.frameout.app.about.AboutScreen
import com.frameout.app.batch.BatchScreen
import com.frameout.app.collage.CollageScreen
import com.frameout.app.compare.CompareScreen
import com.frameout.app.extract.ExtractingScreen
import com.frameout.app.frames.FramesScreen
import com.frameout.app.library.LibraryScreen
import com.frameout.app.picker.PickerScreen
import com.frameout.app.settings.SettingsScreen
import com.frameout.app.trim.TrimScreen
import com.frameout.app.viewer.ViewerScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun RouteContent(
    route: AppRoute,
    navigate: (AppRoute) -> Unit,
    finishExtraction: (Long) -> Unit,
    back: () -> Unit,
    replaceWith: (AppRoute) -> Unit,
    activity: ComponentActivity,
    app: FrameOutApp,
    scope: CoroutineScope
) {
    when (val r = route) {
        AppRoute.Library -> LibraryScreen(
            onOpenProject = { id ->
                scope.launch {
                    val status = runCatching { app.container.projects.getById(id)?.status }.getOrNull()
                    navigate(if (status in ACTIVE_EXTRACTION) AppRoute.Extracting(id) else AppRoute.Frames(id))
                }
            },
            onAddVideo = { navigate(AppRoute.Picker) },
            onOpenVideo = { navigate(AppRoute.Trim(it)) },
            onExtracting = { navigate(AppRoute.Extracting(it)) },
            onSettings = { navigate(AppRoute.Settings) },
            onOpenSaved = { projectId, frameId, savedId -> navigate(AppRoute.Viewer(projectId, frameId, savedId)) },
            onOpenSavedImage = { savedId -> navigate(AppRoute.SavedViewer(savedId)) }
        )
        AppRoute.Settings -> SettingsScreen(onBack = back, onAbout = { navigate(AppRoute.About) }, onLibrary = { replaceWith(AppRoute.Library) }, onAddVideo = { navigate(AppRoute.Picker) })
        AppRoute.About -> AboutScreen(back)
        AppRoute.Picker -> PickerScreen({ navigate(pickerRoute(it)) }, back)
        is AppRoute.Trim -> {
            var starting by remember { mutableStateOf(false) }
            var startError by remember { mutableStateOf<String?>(null) }
            val haptics = com.frameout.designsystem.haptics.rememberFrameHaptics()
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                TrimScreen(r.uri, back, onExtract = { uri, range, autoSave ->
                    if (!starting) {
                        starting = true
                        haptics.perform(com.frameout.designsystem.haptics.FrameHaptic.Confirm)
                        scope.launch {
                            try {
                                startExtraction(activity, app, uri, range, autoSave) { startError = it }?.let { navigate(AppRoute.Extracting(it)) }
                            } finally {
                                starting = false
                            }
                        }
                    }
                })
                com.frameout.designsystem.components.FrameToastHost(
                    startError,
                    Modifier.align(androidx.compose.ui.Alignment.TopCenter),
                    kind = com.frameout.designsystem.components.ToastKind.Error,
                    onDismiss = { startError = null }
                )
                if (starting) {
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
                            .pointerInput(Unit) { detectTapGestures { } },
                        androidx.compose.ui.Alignment.Center
                    ) {
                        PreferredFrameTheme { com.frameout.designsystem.components.FrameSpinner() }
                    }
                }
            }
        }
        is AppRoute.Extracting -> ExtractingScreen(r.projectId, { finishExtraction(r.projectId) }, back, { navigate(AppRoute.Collage(r.projectId, it)) }, { navigate(AppRoute.Viewer(r.projectId, it)) }, { navigate(AppRoute.Compare(r.projectId, it)) }, onLibrary = { replaceWith(AppRoute.Library) }, onSettings = { navigate(AppRoute.Settings) })
        is AppRoute.Frames -> FramesScreen(r.projectId, back, { navigate(AppRoute.Collage(r.projectId, it)) }, { navigate(AppRoute.Viewer(r.projectId, it)) }, { navigate(AppRoute.Compare(r.projectId, it)) }, focusFrameId = r.focusFrameId)
        is AppRoute.Viewer -> ViewerScreen(r.projectId, r.frameId, back, savedId = r.savedId, onShowInProject = { project, frame -> navigate(AppRoute.Frames(project, frame)) })
        is AppRoute.SavedViewer -> com.frameout.app.library.SavedImageViewerScreen(r.savedId, back)
        is AppRoute.Compare -> CompareScreen(r.frameIds, back)
        is AppRoute.Collage -> CollageScreen(r.projectId, r.frameIds, back, back)
        is AppRoute.Batch -> BatchScreen(r.uris, r.session, back, { navigate(AppRoute.Frames(it)) })
    }
}
