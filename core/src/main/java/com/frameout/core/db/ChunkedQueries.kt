package com.frameout.core.db

const val SQL_IN_CHUNK = 500

private fun List<Long>.inChunks(): List<List<Long>> = distinct().chunked(SQL_IN_CHUNK)

suspend fun FrameDao.getByIdsChunked(ids: List<Long>): List<FrameEntity> = ids.inChunks().flatMap { getByIds(it) }.sortedBy { it.index }

suspend fun FrameDao.softDeleteChunked(ids: List<Long>) = ids.inChunks().forEach { softDelete(it) }

suspend fun FrameDao.setFavoriteManyChunked(ids: List<Long>, fav: Boolean) = ids.inChunks().forEach { setFavoriteMany(it, fav) }

suspend fun FrameDao.permanentDeleteChunked(ids: List<Long>) = ids.inChunks().forEach { permanentDelete(it) }

suspend fun FrameDao.restoreChunked(ids: List<Long>) = ids.inChunks().forEach { restore(it) }

suspend fun SavedImageDao.getByIdsChunked(ids: List<Long>): List<SavedImageEntity> = ids.inChunks().flatMap { getByIds(it) }

suspend fun SavedImageDao.deleteManyChunked(ids: List<Long>) = ids.inChunks().forEach { deleteMany(it) }
