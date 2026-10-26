package com.frameout.app.library

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.paging.compose.LazyPagingItems
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameConfirmDialog
import com.frameout.designsystem.icons.FrameIcons

@Composable
internal fun LibraryTop(
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
internal fun LibraryBody(
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
internal fun LibraryOverlays(confirmCancel: Boolean, onCancelDismiss: () -> Unit, pending: DeleteRequest?, onPendingDismiss: () -> Unit, vm: LibraryViewModel) {
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
