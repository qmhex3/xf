package com.frameout.core.storage

import com.frameout.core.db.SavedImageDao
import com.frameout.core.db.SavedImageEntity
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

suspend fun SavedImageDao.insertTracked(storage: StorageManager, entity: SavedImageEntity): Long =
    try {
        insert(entity)
    } catch (e: Exception) {
        withContext(NonCancellable) { storage.deleteGalleryUri(entity.uri) }
        throw e
    }
