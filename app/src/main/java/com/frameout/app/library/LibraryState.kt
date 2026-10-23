package com.frameout.app.library

import com.frameout.core.db.AlbumCount
import com.frameout.core.db.ProjectEntity
import com.frameout.core.db.SavedKind

enum class LibraryTab { Projects, Saved }

enum class LibrarySort { Newest, Oldest, Name, Largest }

class ExportProgress(val done: Int, val total: Int)

class PendingGalleryDelete(val sender: android.content.IntentSender, val ids: List<Long>, val alreadyRemoved: Int) {
    var launched: Boolean = false
}

data class LibraryState(
    val pendingGalleryDelete: PendingGalleryDelete? = null,
    val undoSavedCount: Int = 0,
    val undoProjectCount: Int = 0,
    val tab: LibraryTab = LibraryTab.Projects,
    val projects: List<ProjectEntity> = emptyList(),
    val selectedProjects: Set<Long> = emptySet(),
    val selectedSaved: Set<Long> = emptySet(),
    val loading: Boolean = true,
    val focusId: Long? = null,
    val flashSuccess: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
    val messageSaved: com.frameout.designsystem.icons.SaveKind? = null,
    @androidx.annotation.DrawableRes val messageIcon: Int? = null,
    val savedCount: Int = 0,
    val albums: List<AlbumCount> = emptyList(),
    val selectedAlbum: String? = null,
    val savedKind: SavedKind? = null,
    val savedKindCounts: Map<SavedKind, Int> = emptyMap(),
    val query: String = "",
    val projectQuery: String = "",
    val projectSort: LibrarySort = LibrarySort.Newest,
    val savedSort: LibrarySort = LibrarySort.Newest,
    val columns: Int = 3,
    val exportProgress: ExportProgress? = null,
    val exportSaved: com.frameout.app.trim.SavedCapture? = null,
    val exportNote: String? = null
) {
    val hasUndo: Boolean
        get() = undoSavedCount > 0 || undoProjectCount > 0

    val selectionMode: Boolean
        get() = selectedProjects.isNotEmpty() || selectedSaved.isNotEmpty()

    val visibleProjects: List<ProjectEntity>
        get() {
            val needle = projectQuery.trim()
            val filtered = if (needle.isEmpty()) projects else projects.filter { it.name.contains(needle, ignoreCase = true) }
            return when (projectSort) {
                LibrarySort.Newest -> filtered.sortedByDescending { it.updatedAt }
                LibrarySort.Oldest -> filtered.sortedBy { it.updatedAt }
                LibrarySort.Name -> filtered.sortedBy { it.name.lowercase() }
                LibrarySort.Largest -> filtered.sortedByDescending { it.extractedCount }
            }
        }
}

fun LibraryState.withProjectQuery(query: String): LibraryState {
    val next = copy(projectQuery = query)
    val visible = next.visibleProjects.mapTo(HashSet()) { it.id }
    return next.copy(selectedProjects = next.selectedProjects.filterTo(HashSet()) { it in visible })
}

fun LibraryState.withProjects(list: List<ProjectEntity>): LibraryState {
    val fab = list.map { it.toFab() }
    val existing = list.mapTo(HashSet()) { it.id }
    return copy(
        projects = list,
        selectedProjects = selectedProjects.filterTo(HashSet()) { it in existing },
        loading = false,
        focusId = pickFocus(fab, focusId),
        flashSuccess = flashSuccess || justCompleted(fab, focusId)
    )
}
