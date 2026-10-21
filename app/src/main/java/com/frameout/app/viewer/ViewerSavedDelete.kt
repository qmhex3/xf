package com.frameout.app.viewer

import android.content.IntentSender
import com.frameout.core.db.SavedImageDao
import com.frameout.core.db.SavedImageEntity
import com.frameout.core.storage.GalleryBackup
import com.frameout.core.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SavedConsent(val sender: IntentSender) {
    var launched: Boolean = false
}

internal sealed interface SavedDeleteOutcome {
    class Done(val id: Long, val undoable: Boolean) : SavedDeleteOutcome
    class Consent(val sender: IntentSender) : SavedDeleteOutcome
    object Failed : SavedDeleteOutcome
}

internal class SavedRestored(val id: Long, val index: Int)

internal class ViewerSavedDelete(
    private val storage: StorageManager,
    private val dao: SavedImageDao,
    private val backup: GalleryBackup
) {
    private class Trashed(val entity: SavedImageEntity, val index: Int, val snapshot: GalleryBackup.Snapshot?)

    private val stack = ArrayList<Trashed>()
    private var waiting: Trashed? = null

    val pending: Int get() = stack.size

    suspend fun trash(id: Long, index: Int): SavedDeleteOutcome {
        val entity = dao.getById(id) ?: return SavedDeleteOutcome.Failed
        val snapshot = backup.capture(entity.uri, entity.displayName, entity.createdAt)
        val item = Trashed(entity, index, snapshot)
        if (storage.deleteGalleryUri(entity.uri)) return finish(item)
        val sender = withContext(Dispatchers.IO) { storage.galleryDeleteRequest(listOf(entity.uri)) }
        if (sender == null) {
            backup.discard(snapshot)
            return SavedDeleteOutcome.Failed
        }
        waiting = item
        return SavedDeleteOutcome.Consent(sender)
    }

    suspend fun consent(approved: Boolean): SavedDeleteOutcome? {
        val item = waiting ?: return null
        waiting = null
        if (!approved || !storage.deleteGalleryUri(item.entity.uri)) {
            backup.discard(item.snapshot)
            return SavedDeleteOutcome.Failed
        }
        return finish(item)
    }

    suspend fun restore(): SavedRestored? {
        val item = stack.removeLastOrNull() ?: return null
        val snapshot = item.snapshot ?: return null
        val uri = backup.restore(snapshot) ?: run {
            stack += item
            return null
        }
        dao.insert(item.entity.copy(uri = uri.toString()))
        backup.discard(snapshot)
        return SavedRestored(item.entity.id, item.index)
    }

    fun commit() {
        stack.forEach { backup.discard(it.snapshot) }
        stack.clear()
        waiting?.let { backup.discard(it.snapshot) }
        waiting = null
    }

    private suspend fun finish(item: Trashed): SavedDeleteOutcome {
        dao.delete(item.entity.id)
        if (item.snapshot == null) return SavedDeleteOutcome.Done(item.entity.id, false)
        stack += item
        return SavedDeleteOutcome.Done(item.entity.id, true)
    }
}
