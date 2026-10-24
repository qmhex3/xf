package com.frameout.app.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.asChooserWithoutSelf
import com.frameout.app.frames.ExportResult
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.liveMessage
import com.frameout.designsystem.components.FrameConfirmDialog
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameAmbientGlow

@Composable
fun LibraryScreen(
    onOpenProject: (Long) -> Unit,
    onAddVideo: () -> Unit,
    onOpenVideo: (android.net.Uri) -> Unit = {},
    onExtracting: (Long) -> Unit,
    onSettings: () -> Unit = {},
    onOpenSaved: (projectId: Long, frameId: Long, savedId: Long) -> Unit = { _, _, _ -> },
    onOpenSavedImage: (savedId: Long) -> Unit = {},
    vm: LibraryViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val paging = vm.savedPagingFlow.collectAsLazyPagingItems()
    val deleteConsent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { vm.onGalleryDeleteResult(it.resultCode == android.app.Activity.RESULT_OK) }
    LaunchedEffect(state.pendingGalleryDelete) {
        state.pendingGalleryDelete?.takeIf { !it.launched }?.let {
            it.launched = true
            deleteConsent.launch(androidx.activity.result.IntentSenderRequest.Builder(it.sender).build())
        }
    }
    var pendingDelete by remember { mutableStateOf<DeleteRequest?>(null) }
    var confirmCancel by remember { mutableStateOf(false) }
    val visibleProjects = remember(state.projects, state.projectQuery, state.projectSort) { state.visibleProjects }
    val context = androidx.compose.ui.platform.LocalContext.current
    val listState = vm.listState
    val gridState = vm.gridState
    LibraryEffects(state, vm)
    val suggestion = rememberVideoSuggestion()
    val exporting = state.exportProgress != null
    val clearOrCancel: () -> Unit = { if (exporting) vm.cancelExport() else vm.clearSelection() }
    var exportSheet by remember { mutableStateOf(false) }
    LaunchedEffect(state.selectionMode) { if (!state.selectionMode) exportSheet = false }
    androidx.activity.compose.BackHandler(enabled = state.selectionMode) { clearOrCancel() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importBackup) }
    val exportOne = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> uri?.let(vm::exportToDocument) }
    val exportMany = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(vm::exportToFolder) }
    val openSaved: (Long) -> Unit = remember(vm, onOpenSaved, onOpenSavedImage, context) {
        { savedId ->
            vm.openSaved(
                savedId,
                onViewer = { projectId, frameId -> onOpenSaved(projectId, frameId, savedId) },
                onSavedImage = { onOpenSavedImage(savedId) },
                onSystem = { uri ->
                    val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull() ?: "image/*"
                    val view = android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri, mime).apply {
                        clipData = android.content.ClipData.newRawUri("", uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { context.startActivity(view) }
                }
            )
        }
    }
    val dockActions = remember(vm, onAddVideo, onSettings) {
        LibraryDockActions(onAddVideo, onSettings, vm::pauseFocused, vm::resumeFocused, { confirmCancel = true })
    }
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad by animateDpAsState(navBar + 96.dp, frameSpring(), label = "libraryBottomPad")
    val onProjectsExport: () -> Unit = {
        val ids = state.selectedProjects
        if (ids.size == 1) {
            state.projects.firstOrNull { it.id == ids.first() }
                ?.let { exportOne.launch(com.frameout.core.backup.BackupFormat.fileName(it.name, System.currentTimeMillis())) }
        } else if (ids.size > 1) {
            exportMany.launch(null)
        }
    }
    val inProjects = state.tab == LibraryTab.Projects
    val exportFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { vm.exportSelectedSaved(SavedExportKind.Folder, it) } }
    val selectionDock = if (state.selectionMode) {
        buildSelectionDock(
            SelectionDockInput(
                inProjects = inProjects,
                exporting = exporting,
                onProjectsExport = { exportSheet = true },
                onShare = { vm.shareSelectedSaved { context.startActivity(it.asChooserWithoutSelf(context, FrameStrings.share())) } },
                onSaveToGallery = vm::saveSelectedToGallery,
                onSavedExport = { exportSheet = true },
                onBusy = vm::notifyExportBusy,
                onDelete = { pendingDelete = if (inProjects) DeleteRequest.Projects(state.selectedProjects.size) else DeleteRequest.Saved(state.selectedSaved.size) }
            )
        )
    } else null
    val exportChoices = ProjectExportChoices(
        onBackup = onProjectsExport,
        onZip = { vm.exportProjects(ProjectExportKind.Zip) },
        onVideo = { vm.exportProjects(ProjectExportKind.Video) }
    )
    PreferredFrameTheme {
        val colors = FrameTheme.colors
        Box(Modifier.fillMaxSize().background(colors.background).frameAmbientGlow()) {
            Column(Modifier.fillMaxSize()) {
                LibraryTop(
                    state = state,
                    vm = vm,
                    headerActions = HeaderActions(onClear = clearOrCancel, onSelectAll = { vm.toggleSelectAll() }),
                    onImport = { picker.launch(arrayOf("*/*")) }
                )
                LibraryBody(
                    state = state,
                    visibleProjects = visibleProjects,
                    listState = listState,
                    gridState = gridState,
                    bottomPad = bottomPad,
                    paging = paging,
                    vm = vm,
                    onOpenProject = onOpenProject,
                    onExtracting = onExtracting,
                    onAdd = onAddVideo,
                    onOpenSaved = openSaved,
                    onDelete = { pendingDelete = DeleteRequest.Project(it) }
                )
            }
            BottomScrim(bottomPad, Modifier.align(Alignment.BottomCenter))
            LibraryDock(
                state,
                dockActions,
                Modifier.align(Alignment.BottomCenter),
                selection = selectionDock,
                suggestion = suggestion.ui,
                suggestionVisible = state.message == null,
                onSuggestion = {
                    suggestion.ui?.let { ui ->
                        suggestion.handled()
                        onOpenVideo(ui.video.uri)
                    }
                },
                onSuggestionDismiss = suggestion.handled
            )
            ProjectExportSheet(exportSheet && inProjects && state.selectedProjects.isNotEmpty(), state.selectedProjects.size, exportChoices) { exportSheet = false }
            SavedExportHost(exportSheet && !inProjects && state.selectedSaved.isNotEmpty(), state.selectedSaved.size, vm, { exportFolder.launch(null) }) { exportSheet = false }
            ExportResult(state.exportSaved, vm::clearExport, Modifier.align(Alignment.TopCenter), state.exportNote)
            LibraryOverlays(confirmCancel, { confirmCancel = false }, pendingDelete, { pendingDelete = null }, vm)
            com.frameout.designsystem.components.FrameToastHost(
                state.message,
                Modifier.align(Alignment.TopCenter),
                kind = if (state.messageIsError) com.frameout.designsystem.components.ToastKind.Error else if (state.messageSaved != null) com.frameout.designsystem.components.ToastKind.Success else com.frameout.designsystem.components.ToastKind.Info,
                icon = state.messageIcon,
                saved = state.messageSaved,
                durationMs = if (state.hasUndo) 6000L else null,
                actions = when {
                    state.undoProjectCount > 0 -> listOf(com.frameout.designsystem.components.ToastAction(FrameStrings.undo(), vm::undoProjectDelete))
                    state.undoSavedCount > 0 -> listOf(com.frameout.designsystem.components.ToastAction(FrameStrings.undo(), vm::undoSavedDelete))
                    else -> emptyList()
                },
                onDismiss = vm::clearMessage
            )
        }
    }
}

@Composable
private fun LibraryTop(
    state: LibraryState,
    vm: LibraryViewModel,
    headerActions: HeaderActions,
    onImport: () -> Unit
) {
    val projects = state.tab == LibraryTab.Projects
    val liveCount = state.projects.count { isLive(it.status) }
    LibraryHeader(
        selectedCount = if (projects) state.selectedProjects.size else state.selectedSaved.size,
        selectionMode = state.selectionMode,
        liveCount = liveCount,
        exportProgress = state.exportProgress,
        actions = headerActions
    )
    if (projects) {
        LibraryControls(
            query = state.projectQuery,
            onQuery = vm::setProjectQuery,
            hint = FrameStrings.searchProjects(),
            onImport = onImport,
            sortLabel = state.projectSort.label(),
            sortActive = state.projectSort != LibrarySort.Newest
        ) { dismiss ->
            SortOptions(listOf(LibrarySort.Newest, LibrarySort.Oldest, LibrarySort.Name, LibrarySort.Largest), state.projectSort) {
                vm.setProjectSort(it); dismiss()
            }
        }
    } else {
        LibraryControls(
            query = state.query,
            onQuery = vm::setQuery,
            hint = FrameStrings.searchSaved(),
            onImport = onImport,
            sortLabel = state.savedSort.label(),
            sortActive = state.savedSort != LibrarySort.Newest || state.selectedAlbum != null || state.savedKind != null
        ) { dismiss ->
            SortOptions(listOf(LibrarySort.Newest, LibrarySort.Oldest, LibrarySort.Name), state.savedSort) {
                vm.setSavedSort(it); dismiss()
            }
            SavedKindOptions(state) { vm.setSavedKind(it); dismiss() }
            AlbumOptions(state) { vm.setAlbum(it); dismiss() }
        }
    }
    LibraryTabBar(tab = state.tab, projectCount = state.projects.size, savedCount = state.savedCount, onSelect = vm::setTab)
}

@Composable
private fun LibraryBody(
    state: LibraryState,
    visibleProjects: List<com.frameout.core.db.ProjectEntity>,
    listState: LazyListState,
    gridState: LazyGridState,
    bottomPad: Dp,
    paging: LazyPagingItems<SavedListItem>,
    vm: LibraryViewModel,
    onOpenProject: (Long) -> Unit,
    onExtracting: (Long) -> Unit,
    onAdd: () -> Unit,
    onOpenSaved: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    when {
        state.loading -> LibraryLoading()
        state.tab == LibraryTab.Projects -> if (visibleProjects.isEmpty() && state.projectQuery.isNotBlank()) {
            LibraryEmpty(icon = FrameIcons.Search, title = FrameStrings.noResults(), hint = FrameStrings.noResultsHint())
        } else {
            val actions = remember(vm, onOpenProject, onExtracting, onAdd, onDelete) {
                ProjectActions(
                    onOpen = onOpenProject,
                    onToggle = vm::toggleProject,
                    onResume = { vm.resume(it); onExtracting(it) },
                    onPause = vm::pause,
                    onDelete = onDelete,
                    onAdd = onAdd
                )
            }
            ProjectsPane(visibleProjects, state.selectedProjects, listState, bottomPad, actions)
        }
        else -> SavedPane(state, paging, gridState, bottomPad, onToggle = vm::toggleSaved, onOpen = onOpenSaved, onResave = vm::resave, onColumns = vm::setColumns)
    }
}

@Composable
private fun LibraryOverlays(confirmCancel: Boolean, onCancelDismiss: () -> Unit, pending: DeleteRequest?, onPendingDismiss: () -> Unit, vm: LibraryViewModel) {
    if (confirmCancel) {
        FrameConfirmDialog(
            title = FrameStrings.confirmCancelExtract(),
            body = FrameStrings.confirmCancelExtractBody(),
            confirmLabel = FrameStrings.cancelExtract(),
            dismissLabel = FrameStrings.keepExtracting(),
            onConfirm = { onCancelDismiss(); vm.cancelFocused() },
            onDismiss = onCancelDismiss,
            destructive = true
        )
    }
    pending?.let { request ->
        ConfirmDeleteDialog(
            request = request,
            onConfirm = {
                when (request) {
                    is DeleteRequest.Project -> vm.delete(request.id)
                    is DeleteRequest.Projects -> vm.deleteSelectedProjects()
                    is DeleteRequest.Saved -> vm.deleteSelectedSaved()
                }
                onPendingDismiss()
            },
            onDismiss = onPendingDismiss
        )
    }
}
