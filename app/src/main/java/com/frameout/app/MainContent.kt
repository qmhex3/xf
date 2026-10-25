package com.frameout.app

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.pointer.pointerInput
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.CompositionLocalProvider
import com.frameout.designsystem.motion.LocalThemeWave
import com.frameout.designsystem.motion.ThemeWaveHost
import com.frameout.designsystem.motion.ThemeWaveState
import com.frameout.app.about.AboutScreen
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.app.batch.BatchScreen
import com.frameout.app.collage.CollageScreen
import com.frameout.app.compare.CompareScreen
import com.frameout.app.extract.ExtractingScreen
import com.frameout.app.frames.FramesScreen
import com.frameout.app.library.LibraryScreen
import com.frameout.app.oem.OemGuideScreen
import com.frameout.app.onboarding.OnboardingScreen
import com.frameout.app.picker.PickerScreen
import com.frameout.core.media.LatestVideoFinder
import com.frameout.app.settings.SettingsScreen
import com.frameout.app.trim.TrimScreen
import com.frameout.app.viewer.ViewerScreen
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.CancellationException
import kotlinx.coroutines.launch

@Composable
fun MainContent(activity: ComponentActivity, app: FrameOutApp, incoming: List<Uri> = emptyList(), capture: Boolean = false, onCaptureHandled: () -> Unit = {}, pendingBackup: Uri? = null, onBackupConfirm: (Uri) -> Unit = {}, onBackupDismiss: () -> Unit = {}, notifPrompt: Boolean = false, onNotifAllow: () -> Unit = {}, onNotifDismiss: () -> Unit = {}, onIncomingHandled: () -> Unit = {}) {
    var splash by rememberSaveable { mutableStateOf(incoming.isEmpty() && !capture) }
    val wave = remember { ThemeWaveState() }
    CompositionLocalProvider(LocalThemeWave provides wave) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            MainContentBody(activity, app, incoming, capture, onCaptureHandled, pendingBackup, onBackupConfirm, onBackupDismiss, notifPrompt, onNotifAllow, onNotifDismiss, onIncomingHandled, splashActive = splash)
            ThemeWaveHost(wave)
            IncomingBusyOverlay()
            com.frameout.app.security.IntegrityGate(activity)
            if (splash) com.frameout.app.splash.BrandSplash(fast = app.container.prefs.onboardingDone()) { splash = false }
        }
    }
}

@Composable
private fun MainContentBody(activity: ComponentActivity, app: FrameOutApp, incoming: List<Uri> = emptyList(), capture: Boolean = false, onCaptureHandled: () -> Unit = {}, pendingBackup: Uri? = null, onBackupConfirm: (Uri) -> Unit = {}, onBackupDismiss: () -> Unit = {}, notifPrompt: Boolean = false, onNotifAllow: () -> Unit = {}, onNotifDismiss: () -> Unit = {}, onIncomingHandled: () -> Unit = {}, splashActive: Boolean = false) {
    val stack = rememberSaveable(saver = routeStackSaver) { mutableStateListOf<AppRoute>(AppRoute.Library) }
    var showOem by remember { mutableStateOf(app.container.prefs.recoveryPending() && app.container.device.oemFamily != "stock") }
    var showOnboarding by remember { mutableStateOf(!app.container.prefs.onboardingDone()) }
    var handoff by remember { mutableStateOf(false) }
    val settle = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(handoff) {
        if (handoff) {
            settle.snapTo(1.08f)
            settle.animateTo(1f, androidx.compose.animation.core.tween(900, easing = com.frameout.designsystem.motion.FlowEase.Quint))
        }
    }
    val scope = rememberCoroutineScope()
    var direction by remember { mutableIntStateOf(1) }
    val fab = remember { FabRevealState() }
    val viewerReveal = remember { ViewerRevealState() }
    fun navigate(route: AppRoute) {
        if (stack.lastOrNull() != route) { com.frameout.app.ads.AdsGateway.action(); viewerReveal.onNavigate(stack.lastOrNull(), route); if (isTabSwitch(stack.lastOrNull(), route)) fab.reset() else fab.onNavigate(route); direction = 1; stack.add(route) }
    }
    fun back() {
        if (stack.size > 1) { viewerReveal.onBack(stack.last()); if (isTabSwitch(stack.last(), stack[stack.lastIndex - 1])) fab.reset() else fab.onBack(stack.last()); direction = -1; stack.removeAt(stack.lastIndex); if (stack.last() is AppRoute.Library) com.frameout.app.ads.AdsGateway.screenBreak() }
    }
    fun finishExtraction(projectId: Long) {
        val target = AppRoute.Frames(projectId)
        if (stack.lastOrNull() == target) return
        fab.reset()
        viewerReveal.reset()
        direction = 0
        while (stack.size > 1 && stack.last() !is AppRoute.Library) stack.removeAt(stack.lastIndex)
        stack.add(AppRoute.Frames(projectId))
    }
    fun replaceTop(route: AppRoute) {
        if (isTabSwitch(stack.lastOrNull(), route)) fab.reset() else fab.onReplace(route)
        viewerReveal.reset()
        direction = if (route is AppRoute.Library || route is AppRoute.Settings) 0 else 1
        if (route is AppRoute.Library || route is AppRoute.Settings) {
            stack.clear()
            stack.add(route)
        } else {
            if (stack.size > 1) stack.removeAt(stack.lastIndex)
            stack.add(route)
        }
    }
    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = stack.size > 1 && !showOem) { progress ->
        try {
            progress.collect { backEvent -> backProgress = backEvent.progress.coerceIn(0f, 1f) }
            back()
        } catch (e: CancellationException) {
            throw e
        } finally {
            backProgress = 0f
        }
    }
    LaunchedEffect(PendingOpen.projectId) {
        val id = PendingOpen.projectId
        if (id < 0L) return@LaunchedEffect
        PendingOpen.projectId = -1L
        navigate(AppRoute.Frames(id))
    }
    LaunchedEffect(capture) {
        if (!capture) return@LaunchedEffect
        val latest = LatestVideoFinder.find(activity)?.uri
        val last = app.container.prefs.lastVideoUri()
        val target = latest ?: last.takeIf { it.isNotBlank() }?.let(Uri::parse)
        val route = if (target == null) AppRoute.Picker else pickerRoute(listOf(target))
        onCaptureHandled()
        navigate(route)
    }
    var shareNotice by remember { mutableStateOf<String?>(null) }
    IncomingShareHost(activity, incoming, !showOem && !showOnboarding, onIncomingHandled, { navigate(pickerRoute(it)) }) { shareNotice = it }
    if (showOem) {
        OemGuideScreen(app.container.device.oemFamily) {
            app.container.prefs.setOemGuideDone()
            app.container.prefs.setRecoveryPending(false)
            showOem = false
        }
        return
    }
    if (showOnboarding) {
        OnboardingScreen(
            onAllowNotifications = onNotifAllow,
            ready = !splashActive,
            onStart = {
                app.container.prefs.setOnboardingDone()
                app.container.prefs.setOemGuideDone()
                showOnboarding = false
                handoff = true
                if (!incomingPending()) navigate(AppRoute.Picker)
            },
            onDone = {
                app.container.prefs.setOnboardingDone()
                app.container.prefs.setOemGuideDone()
                showOnboarding = false
                handoff = true
            }
        )
        return
    }
    val backDirection = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize().graphicsLayer {
            translationX = 56.dp.toPx() * backProgress * backDirection
            alpha = 1f - (backProgress * 0.06f)
            scaleX = settle.value
            scaleY = settle.value
        }
    ) {
        val lowEnd = remember { (activity.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager)?.isLowRamDevice == true }
        val reduceMotion = LocalSystemA11y.current.reduceMotion || lowEnd
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        androidx.compose.runtime.SideEffect {
            fab.allowed = !reduceMotion
            viewerReveal.allowed = !reduceMotion
        }
        AnimatedContent(
            targetState = stack.last(),
            contentKey = ::contentKeyOf,
            transitionSpec = { if (zoomKind(initialState, targetState) == 3) routeTransition(0, rtl, reduceMotion, 3) else if (fab.active && !reduceMotion) fabTransition(fab.opening) else if (viewerReveal.active && !reduceMotion) fabTransition(viewerReveal.opening) else routeTransition(direction, rtl, reduceMotion, zoomKind(initialState, targetState)) },
            label = "route"
        ) { route ->
            FabRevealHost(route, fab) { ViewerRevealHost(route, viewerReveal) { RouteContent(route, ::navigate, ::finishExtraction, ::back, ::replaceTop, activity, app, scope) } }
        }
        com.frameout.designsystem.components.FrameToastHost(
            shareNotice,
            Modifier.align(androidx.compose.ui.Alignment.TopCenter),
            kind = com.frameout.designsystem.components.ToastKind.Error,
            onDismiss = { shareNotice = null }
        )
    }
    if (handoff) {
        PreferredFrameTheme {
            com.frameout.designsystem.motion.IrisReveal(
                playing = true,
                color = com.frameout.designsystem.theme.FrameTheme.colors.background,
                ring = com.frameout.designsystem.theme.FrameTheme.accent.a,
                onFinished = { handoff = false }
            )
        }
    }
    if (notifPrompt && pendingBackup == null) {
        com.frameout.designsystem.components.FrameConfirmDialog(
            title = com.frameout.core.util.FrameStrings.notifRationaleTitle(),
            body = com.frameout.core.util.FrameStrings.notifRationaleBody(),
            confirmLabel = com.frameout.core.util.FrameStrings.notifAllow(),
            dismissLabel = com.frameout.core.util.FrameStrings.notifNotNow(),
            onConfirm = onNotifAllow,
            onDismiss = onNotifDismiss
        )
    }
    com.frameout.app.rating.RatingPromptHost(app, stack.lastOrNull(), blocked = notifPrompt || pendingBackup != null)
    if (pendingBackup != null) {
        com.frameout.designsystem.components.FrameConfirmDialog(
            title = com.frameout.core.util.FrameStrings.backupConfirmTitle(),
            body = com.frameout.core.util.FrameStrings.backupConfirmBody(com.frameout.app.batch.BatchNaming.displayName(app, pendingBackup.toString())),
            confirmLabel = com.frameout.core.util.FrameStrings.importProject(),
            dismissLabel = com.frameout.core.util.FrameStrings.cancel(),
            onConfirm = { onBackupConfirm(pendingBackup) },
            onDismiss = onBackupDismiss, icon = com.frameout.designsystem.icons.FrameIconsExtra.Import
        )
    }
}

@Composable
private fun RouteContent(
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
