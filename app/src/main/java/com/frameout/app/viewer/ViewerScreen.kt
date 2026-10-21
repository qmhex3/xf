package com.frameout.app.viewer

import com.frameout.app.frames.isStatusError
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.frameout.app.asChooserWithoutSelf
import com.frameout.designsystem.a11y.liveMessage
import com.frameout.core.util.FrameA11y
import com.frameout.core.util.FrameStrings
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.VolumeKeyNavigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameColors
import com.frameout.designsystem.theme.FrameTheme

private const val PEEK_SCALE = 3f

@Composable
fun ViewerScreen(
    projectId: Long,
    frameId: Long,
    onClose: () -> Unit,
    savedId: Long? = null,
    vm: ViewerViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    com.frameout.app.billing.PaywallSampleEffect(state.bitmap)
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val pagerState = rememberViewerPager(vm)
    val held = rememberHeldNeighbors()
    held.sync(pagerState, state)
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { vm.onSavedConsent(it.resultCode == android.app.Activity.RESULT_OK) }
    LaunchedEffect(state.savedConsent) {
        state.savedConsent?.takeIf { !it.launched }?.let {
            it.launched = true
            consent.launch(IntentSenderRequest.Builder(it.sender).build())
        }
    }
    val arrows = rememberArrowWake(state.current?.id, com.frameout.designsystem.a11y.LocalSystemA11y.current.reduceMotion)

    BackHandler(enabled = state.tool != EditTool.None) { if (state.discardPrompt) vm.tools.keepEditing() else if (!state.toolBusy) vm.tools.requestCancel() }
    VolumeKeyNavigation(!state.editing) { forward -> vm.goTo(if (forward) 1 else -1) }
    LaunchedEffect(projectId, frameId, savedId) { vm.open(projectId, frameId, savedId) }
    LaunchedEffect(state.message, state.messageBusy) {
        if (state.message != null && !state.messageBusy) {
            kotlinx.coroutines.delay(com.frameout.designsystem.components.toastDurationMs(state.message.orEmpty(), com.frameout.designsystem.components.ToastKind.Success))
            vm.clearMessage()
        }
    }
    LaunchedEffect(state.deletedStack, state.savedUndoCount) {
        if (state.hasUndo) {
            kotlinx.coroutines.delay(6000)
            vm.dismissUndo()
        }
    }
    LaunchedEffect(state.current, state.loading, state.hasUndo, state.savedConsent) {
        if (state.current == null && !state.loading && !state.hasUndo && state.savedConsent == null) onClose()
    }

    PreferredFrameTheme {
        val colors = FrameTheme.colors
        Box(Modifier.fillMaxSize().background(FrameColors.ViewerBlack).wakeOnTouch(arrows)) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !state.editing && !state.playing && state.scale <= 1.05f,
                beyondViewportPageCount = 1,
                flingBehavior = viewerFling(pagerState),
                modifier = Modifier.fillMaxSize().padding(state.imagePadding()).savedStepActions(state.savedMode, vm).clip(RoundedCornerShape(if (state.controlsVisible && state.tool == EditTool.None) ViewerImageRadius else 0.dp))
            ) { page ->
                PagerPage(pagerState, page) {
                when (page) {
                    0 -> ViewerNeighborImage(
                        held.previousFor(pagerState, state),
                        state.previous?.let { FrameA11y.frame(it.index + 1, formatPts(it.ptsUs)) }
                    )
                    1 -> Box(Modifier.fillMaxSize()) {
                      ViewerImage(
                        bitmap = state.bitmap ?: state.placeholder,
                        fadeKey = state.current?.id,
                        scale = state.scale,
                        offsetX = state.offsetX,
                        offsetY = state.offsetY,
                        cropMode = state.tool.ownsGestures,
                        transformNow = vm::transformNow,
                        colorFilter = state.previewFilter(),
                        description = state.current?.let { FrameA11y.frame(it.index + 1, formatPts(it.ptsUs)) },
                        onTransform = { s, x, y -> vm.setTransform(s, x, y) },
                        onSwipeDown = { if (!state.editing) onClose() },
                        liveText = state.liveText,
                        onLiveSelect = vm::liveSelect,
                        onLiveTap = vm::liveTapWord,
                        onTap = {
                            if (state.playing) vm.stopPlay()
                            else if (state.liveText.active && !state.editing) vm.liveClearSelection()
                            else if (!state.editing) vm.toggleControls()
                        },
                        onLongPress = { press, center, area ->
                            if (state.scale > 1.05f) vm.setTransform(1f, 0f, 0f)
                            else {
                                val limitX = area.x * (PEEK_SCALE - 1f) / 2f
                                val limitY = area.y * (PEEK_SCALE - 1f) / 2f
                                vm.setTransform(
                                    PEEK_SCALE,
                                    ((center.x - press.x) * (PEEK_SCALE - 1f)).coerceIn(-limitX, limitX),
                                    ((center.y - press.y) * (PEEK_SCALE - 1f)).coerceIn(-limitY, limitY)
                                )
                            }
                        }
                      )
                      val shown = state.bitmap
                      if (state.liveText.active && !state.editing && !state.playing && shown != null) {
                          LiveTextOverlay(state.liveText, shown.width, shown.height, state.scale, state.offsetX, state.offsetY)
                      }
                      if (state.showDetections && !state.editing && !state.playing && shown != null) {
                          DetectionOverlay(state.detections, shown.width, shown.height, state.scale, state.offsetX, state.offsetY)
                      }
                    }
                    2 -> ViewerNeighborImage(
                        held.nextFor(pagerState, state),
                        state.next?.let { FrameA11y.frame(it.index + 1, formatPts(it.ptsUs)) }
                    )
                }
                }
            }
            ViewerEditLayer(state, vm)
            if (state.controlsVisible) {
                ViewerChrome(
                    state = state,
                    onSave = { vm.save() },
                    onDelete = { vm.softDelete() },
                    onShare = { vm.shareIntent { context.startActivity(it.asChooserWithoutSelf(context, FrameStrings.share())) } },
                    onSearch = { searchOpen = true },
                    onPlay = { vm.togglePlay() },
                    onSpeed = { vm.cyclePlaySpeed() },
                    onEdit = { tool -> vm.openTools(tool) },
                    onToggleDetections = { vm.toggleDetections() },
                    onToggleLiveText = { vm.toggleLiveText() },
                    onLiveCopy = { vm.liveCopy() },
                    onLiveSelectAll = { vm.liveSelectAll() },
                    onPrevious = { steps -> vm.goTo(-steps) },
                    onNext = { steps -> vm.goTo(steps) },
                    arrows = arrows,
                    tools = vm.tools
                )
            }
            if (state.toolBusy) {
                Box(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    }
                )
            }
            if (state.discardPrompt) {
                com.frameout.designsystem.components.FrameConfirmDialog(
                    title = com.frameout.core.util.EditStrings.discardTitle(),
                    body = com.frameout.core.util.EditStrings.discardBody(),
                    confirmLabel = com.frameout.core.util.EditStrings.discard(),
                    dismissLabel = com.frameout.core.util.EditStrings.keepEditing(),
                    onConfirm = vm.tools::cancel,
                    onDismiss = vm.tools::keepEditing,
                    destructive = true
                )
            }
            ViewerUndoBar(state.hasUndo, state.savedUndoCount > 0, vm::undoLast, Modifier.align(Alignment.TopCenter))
            com.frameout.app.search.ImageSearchPanel(searchOpen, { engine -> searchOpen = false; vm.search(context, engine) }, { searchOpen = false })
            ViewerQualityIndicator(if (state.message == null) state.qualityPhase() else QualityPhase.None, Modifier.align(Alignment.TopCenter))
            com.frameout.designsystem.components.FrameToastHost(
                state.message,
                Modifier.align(Alignment.TopCenter),
                kind = if (state.message?.let { isStatusError(it) } == true) com.frameout.designsystem.components.ToastKind.Error else com.frameout.designsystem.components.ToastKind.Success,
                icon = state.messageIcon,
                saved = state.messageSaved,
                busy = state.messageBusy,
                onDismiss = vm::clearMessage
            )
        }
    }
}
