package com.frameout.app.frames

import android.graphics.Bitmap
import com.frameout.core.db.ProjectEntity
import com.frameout.core.db.TagCount
import com.frameout.core.edit.CropRatio
import com.frameout.core.quality.FrameIntegrity
import com.frameout.core.util.QualityStrings

data class CropAllState(
    val bitmap: Bitmap,
    val count: Int,
    val l: Float = 0f,
    val t: Float = 0f,
    val r: Float = 1f,
    val b: Float = 1f
) {
    fun withRect(nl: Float, nt: Float, nr: Float, nb: Float): CropAllState {
        val rect = CropRatio.clamp(nl, nt, nr, nb, MIN_SIDE)
        return copy(l = rect.l, t = rect.t, r = rect.r, b = rect.b)
    }

    companion object {
        const val MIN_SIDE = 0.05f
    }
}

enum class GridFit { Fill, Fit }
enum class FrameFilter { All, Favorites, Deleted, Albums, Tagged, Sharpest }

data class GallerySaveProgress(val done: Int, val total: Int)

data class FramesState(
    val pendingGalleryDelete: com.frameout.app.library.PendingGalleryDelete? = null,
    val project: ProjectEntity? = null,
    val frameCount: Int = 0,
    val allCount: Int = 0,
    val favoriteCount: Int = 0,
    val deletedCount: Int = 0,
    val toolsOpen: Boolean = false,
    val sharpestVersion: Int = 0,
    val sharpestCount: Int = -1,
    val selected: Set<Long> = emptySet(),
    val fit: GridFit = GridFit.Fill,
    val filter: FrameFilter = FrameFilter.All,
    val loading: Boolean = true,
    val reassembling: Boolean = false,
    val analyzing: Boolean = false,
    val integrity: FrameIntegrity.Report? = null,
    val cropAll: CropAllState? = null,
    val integrityOpen: Boolean = false,
    val tagFilter: Set<String> = emptySet(),
    val tagsOpen: Boolean = false,
    val tagCounts: List<TagCount> = emptyList(),
    val refineCounts: List<TagCount> = emptyList(),
    val scanning: Boolean = false,
    val scanDone: Int = 0,
    val scanTotal: Int = 0,
    val scanFailed: Int = 0,
    val message: String? = null,
    val busyMessage: String? = null,
    val messageSaved: com.frameout.designsystem.icons.SaveKind? = null,
    @androidx.annotation.DrawableRes val messageIcon: Int? = null,
    val undoDelete: List<Long> = emptyList(),
    val confirmGallerySave: Int = 0,
    val undoGalleryIds: List<Long> = emptyList(),
    val gallerySave: GallerySaveProgress? = null,
    val exportSaved: com.frameout.app.trim.SavedCapture? = null
) {
    val messageBusy: Boolean get() = message != null && message == busyMessage
    val selectionMode: Boolean get() = selected.isNotEmpty()
    val isDeletedView: Boolean get() = filter == FrameFilter.Deleted
    val isAlbumsView: Boolean get() = filter == FrameFilter.Albums
}

fun FramesState.toggle(id: Long): FramesState {
    val next = selected.toMutableSet()
    if (!next.add(id)) next.remove(id)
    return copy(selected = next)
}

fun FramesState.clearSelection(): FramesState = copy(selected = emptySet())

fun FramesState.analysisStarted(): FramesState = copy(analyzing = true, message = QualityStrings.analyzing(), messageIcon = null)

fun FramesState.analysisDone(ids: Set<Long>, text: String, icon: Int? = null): FramesState = copy(analyzing = false, selected = ids, message = text, messageIcon = icon)
