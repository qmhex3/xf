package com.frameout.core.storage

import com.frameout.core.db.SavedImageDao
import com.frameout.core.db.SavedImageEntity

class SavedTrash(
    private val dao: SavedImageDao,
    private val backup: GalleryBackup
) {
    private class Entry(val entity: SavedImageEntity, val snapshot: GalleryBackup.Snapshot)

    private val pending = HashMap<Long, GalleryBackup.Snapshot>()
    private val removed = ArrayList<Entry>()

    val undoable: Int get() = removed.size

    suspend fun begin(entities: List<SavedImageEntity>) {
        commit()
        var budget = MAX_BYTES
        for (entity in entities.take(MAX_ITEMS)) {
            val snapshot = backup.capture(entity.uri, entity.displayName, entity.createdAt, budget) ?: continue
            val size = snapshot.file.length()
            if (size > budget) {
                backup.discard(snapshot)
                continue
            }
            budget -= size
            pending[entity.id] = snapshot
        }
    }

    fun markGone(entities: List<SavedImageEntity>) {
        entities.forEach { entity ->
            val snapshot = pending.remove(entity.id) ?: return@forEach
            removed += Entry(entity, snapshot)
        }
    }

    fun release() {
        pending.values.forEach { backup.discard(it) }
        pending.clear()
    }

    suspend fun restoreAll(): Int {
        release()
        val entries = ArrayList(removed)
        removed.clear()
        var restored = 0
        entries.forEach { entry ->
            val uri = backup.restore(entry.snapshot)
            if (uri != null) {
                try {
                    dao.insert(entry.entity.copy(uri = uri.toString()))
                    restored++
                } catch (e: Exception) {
                    com.frameout.core.util.logFailure("SavedTrash", e)
                }
            }
            backup.discard(entry.snapshot)
        }
        return restored
    }

    fun commit() {
        release()
        removed.forEach { backup.discard(it.snapshot) }
        removed.clear()
    }

    companion object {
        const val MAX_ITEMS = 40
        private const val MAX_BYTES = 96L * 1024L * 1024L
    }
}
