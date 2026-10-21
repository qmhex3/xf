package com.frameout.core.db

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

data class FrameKey(val id: Long, val frameIndex: Int)

@Dao
interface FrameDao {
    @Query("SELECT * FROM frames WHERE projectId = :projectId AND isDeleted = 0 ORDER BY `index` ASC")
    fun pagingActive(projectId: Long): PagingSource<Int, FrameEntity>
    @Query("SELECT * FROM frames WHERE projectId = :projectId AND isFavorite = 1 AND isDeleted = 0 ORDER BY `index` ASC")
    fun pagingFavorites(projectId: Long): PagingSource<Int, FrameEntity>
    @Query("SELECT * FROM frames WHERE projectId = :projectId AND isDeleted = 1 ORDER BY `index` ASC")
    fun pagingDeleted(projectId: Long): PagingSource<Int, FrameEntity>
    @Query("SELECT * FROM frames WHERE projectId = :projectId AND isDeleted = 0 ORDER BY `index` ASC LIMIT :limit OFFSET :offset")
    suspend fun pageActive(projectId: Long, limit: Int, offset: Int): List<FrameEntity>
    @Query("SELECT * FROM frames WHERE projectId = :projectId AND `index` > :after ORDER BY `index` ASC LIMIT :limit")
    suspend fun pageAllAfter(projectId: Long, after: Int, limit: Int): List<FrameEntity>
    @Query("SELECT id AS id, `index` AS frameIndex FROM frames WHERE projectId = :projectId")
    suspend fun idsByIndex(projectId: Long): List<FrameKey>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<FrameEntity>)
    @Query("UPDATE frames SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)
    @Query("UPDATE frames SET isFavorite = :fav WHERE id IN (:ids)")
    suspend fun setFavoriteMany(ids: List<Long>, fav: Boolean)
    @Query("UPDATE frames SET isDeleted = 1 WHERE id IN (:ids)")
    suspend fun softDelete(ids: List<Long>)
    @Query("UPDATE frames SET isDeleted = 0 WHERE id IN (:ids)")
    suspend fun restore(ids: List<Long>)
    @Query("DELETE FROM frames WHERE id IN (:ids)")
    suspend fun permanentDelete(ids: List<Long>)
    @Query("DELETE FROM frames WHERE projectId = :projectId AND isDeleted = 1")
    suspend fun emptyTrash(projectId: Long)
    @Query("SELECT * FROM frames WHERE id = :id")
    suspend fun getById(id: Long): FrameEntity?
    @Query("SELECT * FROM frames WHERE id = :id AND isDeleted = 0")
    suspend fun getActiveById(id: Long): FrameEntity?
    @Query("SELECT * FROM frames WHERE projectId = :projectId AND isDeleted = 0 AND `index` > :index ORDER BY `index` ASC LIMIT 1")
    suspend fun nextActive(projectId: Long, index: Int): FrameEntity?
    @Query("SELECT * FROM frames WHERE projectId = :projectId AND isDeleted = 0 AND `index` < :index ORDER BY `index` DESC LIMIT 1")
    suspend fun previousActive(projectId: Long, index: Int): FrameEntity?
    @Query("SELECT * FROM frames WHERE projectId = :projectId AND isDeleted = 0 AND `index` > :index ORDER BY `index` ASC LIMIT :limit")
    suspend fun nextActiveList(projectId: Long, index: Int, limit: Int): List<FrameEntity>
    @Query("SELECT * FROM frames WHERE projectId = :projectId AND isDeleted = 0 AND `index` < :index ORDER BY `index` DESC LIMIT :limit")
    suspend fun previousActiveList(projectId: Long, index: Int, limit: Int): List<FrameEntity>
    @Query("SELECT * FROM frames WHERE id IN (:ids) ORDER BY `index` ASC")
    suspend fun getByIds(ids: List<Long>): List<FrameEntity>
    @Query("UPDATE frames SET fullPath = :path WHERE id = :id")
    suspend fun setFullPath(id: Long, path: String)
    @Query("SELECT * FROM frames WHERE projectId = :projectId ORDER BY `index` DESC LIMIT 1")
    suspend fun lastFrame(projectId: Long): FrameEntity?
    @Query("SELECT * FROM frames WHERE projectId = :projectId ORDER BY `index` DESC LIMIT :limit")
    suspend fun lastFrames(projectId: Long, limit: Int): List<FrameEntity>
    @Query("SELECT COUNT(*) FROM frames WHERE projectId = :projectId")
    suspend fun countAll(projectId: Long): Int
    @Query("SELECT COUNT(*) FROM frames WHERE projectId = :projectId AND isDeleted = 0")
    suspend fun countActive(projectId: Long): Int
    @Query("SELECT COUNT(*) FROM frames WHERE projectId = :projectId AND isDeleted = 0 AND `index` < :index")
    suspend fun countActiveBefore(projectId: Long, index: Int): Int
    @Query("SELECT COUNT(*) FROM frames WHERE projectId = :projectId AND isFavorite = 1 AND isDeleted = 0")
    suspend fun countFavorites(projectId: Long): Int
    @Query("SELECT COUNT(*) FROM frames WHERE projectId = :projectId AND isDeleted = 1")
    suspend fun countDeleted(projectId: Long): Int
    @Query("SELECT id FROM frames WHERE id IN (:ids)")
    suspend fun existingIds(ids: List<Long>): List<Long>
    @Query("SELECT ptsUs FROM frames WHERE projectId = :projectId AND isDeleted = :deleted AND (:favoritesOnly = 0 OR isFavorite = 1) ORDER BY `index` ASC LIMIT 1 OFFSET :offset")
    suspend fun ptsAtOffset(projectId: Long, deleted: Boolean, favoritesOnly: Boolean, offset: Int): Long?
    @Query("SELECT ptsUs FROM frames WHERE projectId = :projectId ORDER BY `index` ASC")
    suspend fun listPts(projectId: Long): List<Long>
    @Query("SELECT id FROM frames WHERE projectId = :projectId AND isDeleted = 0 ORDER BY `index` ASC")
    suspend fun listActiveIds(projectId: Long): List<Long>
    @Query("SELECT id FROM frames WHERE projectId = :projectId AND isFavorite = 1 AND isDeleted = 0 ORDER BY `index` ASC")
    suspend fun listFavoriteIds(projectId: Long): List<Long>
    @Query("SELECT id FROM frames WHERE projectId = :projectId AND isDeleted = 1 ORDER BY `index` ASC")
    suspend fun listDeletedIds(projectId: Long): List<Long>
}
