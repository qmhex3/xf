package com.frameout.core.edit

import androidx.room3.withWriteTransaction
import com.frameout.core.db.AppDatabase
import com.frameout.core.db.FrameEditEntity
import com.frameout.core.db.FrameEditHistoryEntity
import com.frameout.core.db.SQL_IN_CHUNK

class EditHistoryRepository(private val db: AppDatabase) {
    private val dao = db.frameEditDao()
    private val frames = db.frameDao()

    suspend fun save(edit: FrameEditEntity): FrameEditEntity = db.withWriteTransaction {
        val current = dao.get(edit.frameId)
        val revision = current?.revision ?: 0
        if (current != null && dao.historyAt(edit.frameId, revision) == null) dao.insertHistory(current.toHistory())
        if (current == null && dao.historyAt(edit.frameId, 0) == null) dao.insertHistory(FrameEditEntity(edit.frameId).toHistory())
        dao.deleteFuture(edit.frameId, revision)
        val saved = edit.copy(revision = revision + 1, isDeleted = current?.isDeleted ?: edit.isDeleted)
        dao.upsert(saved)
        dao.insertHistory(saved.toHistory())
        saved
    }

    suspend fun delete(frameId: Long): FrameEditEntity = db.withWriteTransaction {
        val current = dao.get(frameId) ?: FrameEditEntity(frameId)
        val revision = current.revision
        if (dao.historyAt(frameId, revision) == null) dao.insertHistory(current.toHistory())
        dao.deleteFuture(frameId, revision)
        val saved = current.copy(revision = revision + 1, isDeleted = true)
        dao.upsert(saved)
        dao.insertHistory(saved.toHistory())
        frames.softDelete(listOf(frameId))
        saved
    }

    suspend fun restore(frameId: Long): FrameEditEntity = db.withWriteTransaction {
        val current = dao.get(frameId) ?: FrameEditEntity(frameId)
        val revision = current.revision
        if (dao.historyAt(frameId, revision) == null) dao.insertHistory(current.toHistory())
        dao.deleteFuture(frameId, revision)
        val saved = current.copy(revision = revision + 1, isDeleted = false)
        dao.upsert(saved)
        dao.insertHistory(saved.toHistory())
        frames.restore(listOf(frameId))
        saved
    }

    suspend fun undo(frameId: Long): FrameEditEntity? = db.withWriteTransaction {
        val current = dao.get(frameId) ?: return@withWriteTransaction null
        val target = dao.previous(frameId, current.revision) ?: return@withWriteTransaction null
        dao.upsert(target.toEdit())
        if (target.isDeleted) frames.softDelete(listOf(frameId)) else frames.restore(listOf(frameId))
        target.toEdit()
    }

    suspend fun redo(frameId: Long): FrameEditEntity? = db.withWriteTransaction {
        val current = dao.get(frameId) ?: return@withWriteTransaction null
        val target = dao.next(frameId, current.revision) ?: return@withWriteTransaction null
        dao.upsert(target.toEdit())
        if (target.isDeleted) frames.softDelete(listOf(frameId)) else frames.restore(listOf(frameId))
        target.toEdit()
    }

    suspend fun deleteMany(frameIds: List<Long>) = setDeletedMany(frameIds, true)

    suspend fun restoreMany(frameIds: List<Long>) = setDeletedMany(frameIds, false)

    suspend fun transformMany(frameIds: List<Long>, transform: (FrameEdit) -> FrameEdit) {
        val ids = frameIds.distinct()
        if (ids.isEmpty()) return
        db.withWriteTransaction {
            ids.chunked(SQL_IN_CHUNK).forEach { chunk ->
                val present = frames.existingIds(chunk)
                if (present.isEmpty()) return@forEach
                val currents = dao.getMany(present).associateBy { it.frameId }
                dao.insertHistoryIgnore(present.map { (currents[it] ?: FrameEditEntity(it)).toHistory() })
                dao.deleteFutureMany(present)
                val saved = present.map { id ->
                    val current = currents[id]
                    val edit = transform(current?.toEdit() ?: FrameEdit(frameId = id)).toEntity()
                    edit.copy(frameId = id, revision = (current?.revision ?: 0) + 1, isDeleted = current?.isDeleted ?: false)
                }
                dao.upsertMany(saved)
                dao.insertHistoryAll(saved.map { it.toHistory() })
            }
        }
    }

    suspend fun undoMany(frameIds: List<Long>) = stepMany(frameIds, true)

    suspend fun redoMany(frameIds: List<Long>) = stepMany(frameIds, false)

    private suspend fun stepMany(frameIds: List<Long>, backward: Boolean) {
        val ids = frameIds.distinct()
        if (ids.isEmpty()) return
        db.withWriteTransaction {
            val deleted = ArrayList<Long>()
            val restored = ArrayList<Long>()
            val targets = ArrayList<FrameEditEntity>()
            ids.forEach { id ->
                val current = dao.get(id) ?: return@forEach
                val target = (if (backward) dao.previous(id, current.revision) else dao.next(id, current.revision)) ?: return@forEach
                targets += target.toEdit()
                if (target.isDeleted) deleted += id else restored += id
            }
            if (targets.isNotEmpty()) dao.upsertMany(targets)
            deleted.chunked(SQL_IN_CHUNK).forEach { frames.softDelete(it) }
            restored.chunked(SQL_IN_CHUNK).forEach { frames.restore(it) }
        }
    }

    private suspend fun setDeletedMany(frameIds: List<Long>, deleted: Boolean) {
        val ids = frameIds.distinct()
        if (ids.isEmpty()) return
        db.withWriteTransaction {
            ids.chunked(SQL_IN_CHUNK).forEach { chunk ->
                val present = frames.existingIds(chunk)
                if (present.isEmpty()) return@forEach
                val currents = dao.getMany(present).associateBy { it.frameId }
                val bases = present.map { currents[it] ?: FrameEditEntity(it) }
                dao.insertHistoryIgnore(bases.map { it.toHistory() })
                dao.deleteFutureMany(present)
                val saved = bases.map { it.copy(revision = it.revision + 1, isDeleted = deleted) }
                dao.upsertMany(saved)
                dao.insertHistoryAll(saved.map { it.toHistory() })
                if (deleted) frames.softDelete(present) else frames.restore(present)
            }
        }
    }

    suspend fun current(frameId: Long): FrameEditEntity? = dao.get(frameId)
}

private fun FrameEditEntity.toHistory() = FrameEditHistoryEntity(
    frameId = frameId, revision = revision, rotation = rotation, flipH = flipH, flipV = flipV,
    cropL = cropL, cropT = cropT, cropR = cropR, cropB = cropB, brightness = brightness,
    contrast = contrast, saturation = saturation, isDeleted = isDeleted, marks = marks,
    filter = filter, filterStrength = filterStrength
)

private fun FrameEditHistoryEntity.toEdit() = FrameEditEntity(
    frameId = frameId, rotation = rotation, flipH = flipH, flipV = flipV,
    cropL = cropL, cropT = cropT, cropR = cropR, cropB = cropB, brightness = brightness,
    contrast = contrast, saturation = saturation, revision = revision, isDeleted = isDeleted, marks = marks,
    filter = filter, filterStrength = filterStrength
)
