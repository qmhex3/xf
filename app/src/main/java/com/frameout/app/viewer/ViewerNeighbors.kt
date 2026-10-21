package com.frameout.app.viewer

import android.graphics.Bitmap
import com.frameout.core.db.FrameDao
import com.frameout.core.db.FrameEntity
import com.frameout.core.edit.EditHistoryRepository
import com.frameout.core.edit.FrameEdit
import com.frameout.core.edit.toEdit
import com.frameout.core.util.EditedThumb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ViewerNeighbors(
    val previous: FrameEntity?,
    val next: FrameEntity?,
    val previousBitmap: Bitmap?,
    val nextBitmap: Bitmap?
) {
    companion object {
        suspend fun load(dao: FrameDao, history: EditHistoryRepository, frame: FrameEntity, px: Int): ViewerNeighbors = withContext(Dispatchers.IO) {
            val previous = dao.previousActive(frame.projectId, frame.index)
            val next = dao.nextActive(frame.projectId, frame.index)
            ViewerNeighbors(previous, next, thumbOf(history, previous, px), thumbOf(history, next, px))
        }

        suspend fun of(history: EditHistoryRepository, previous: FrameEntity?, next: FrameEntity?, px: Int): ViewerNeighbors = withContext(Dispatchers.IO) {
            ViewerNeighbors(previous, next, thumbOf(history, previous, px), thumbOf(history, next, px))
        }

        private suspend fun thumbOf(history: EditHistoryRepository, frame: FrameEntity?, px: Int): Bitmap? {
            frame ?: return null
            val edit = history.current(frame.id)?.toEdit()
            return EditedThumb.peekSharp(frame.fullPath, edit, px)
                ?: EditedThumb.loadSharp(frame.fullPath, edit, px)
                ?: EditedThumb.load(frame.thumbnailPath ?: frame.fullPath, edit)
        }
    }
}

internal fun ViewerState.withNeighbors(n: ViewerNeighbors) =
    copy(previous = n.previous, next = n.next, previousBitmap = n.previousBitmap, nextBitmap = n.nextBitmap)

internal fun ViewerState.withEdit(edit: FrameEdit) =
    copy(edit = edit, cropL = edit.cropL, cropT = edit.cropT, cropR = edit.cropR, cropB = edit.cropB)
