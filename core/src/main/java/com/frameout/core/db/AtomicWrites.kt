package com.frameout.core.db

import androidx.room3.withWriteTransaction

suspend fun AppDatabase.setFavoriteAtomic(ids: List<Long>, favorite: Boolean) {
    if (ids.isEmpty()) return
    withWriteTransaction { frameDao().setFavoriteManyChunked(ids, favorite) }
}

suspend fun AppDatabase.permanentDeleteAtomic(ids: List<Long>) {
    if (ids.isEmpty()) return
    withWriteTransaction { frameDao().permanentDeleteChunked(ids) }
}
