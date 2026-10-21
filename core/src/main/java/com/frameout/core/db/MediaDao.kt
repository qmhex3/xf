package com.frameout.core.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.paging.PagingSource
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedImageDao {
    @Query("SELECT * FROM saved_images ORDER BY album ASC, createdAt DESC")
    fun pagingAll(): PagingSource<Int, SavedImageEntity>
    @Query("SELECT * FROM saved_images WHERE (:album IS NULL OR album = :album) AND (:query = '' OR displayName LIKE '%' || :query || '%' ESCAPE '\\' OR album LIKE '%' || :query || '%' ESCAPE '\\') ORDER BY album ASC, createdAt DESC")
    fun pagingFiltered(album: String?, query: String): PagingSource<Int, SavedImageEntity>
    @Query("SELECT * FROM saved_images WHERE (:album IS NULL OR album = :album) AND (:query = '' OR displayName LIKE '%' || :query || '%' ESCAPE '\\' OR album LIKE '%' || :query || '%' ESCAPE '\\') AND (:kind < 0 OR " + SAVED_KIND_SQL + " = :kind) ORDER BY album ASC, CASE WHEN :sort = 1 THEN createdAt END ASC, CASE WHEN :sort = 2 THEN LOWER(displayName) END ASC, createdAt DESC")
    fun pagingSorted(album: String?, query: String, sort: Int, kind: Int): PagingSource<Int, SavedImageEntity>
    @Query("SELECT id FROM saved_images WHERE (:album IS NULL OR album = :album) AND (:query = '' OR displayName LIKE '%' || :query || '%' ESCAPE '\\' OR album LIKE '%' || :query || '%' ESCAPE '\\') AND (:kind < 0 OR " + SAVED_KIND_SQL + " = :kind) ORDER BY album ASC, CASE WHEN :sort = 1 THEN createdAt END ASC, CASE WHEN :sort = 2 THEN LOWER(displayName) END ASC, createdAt DESC")
    suspend fun orderedIds(album: String?, query: String, sort: Int, kind: Int): List<Long>
    @Query("SELECT id FROM saved_images WHERE (:album IS NULL OR album = :album) AND (:query = '' OR displayName LIKE '%' || :query || '%' ESCAPE '\\' OR album LIKE '%' || :query || '%' ESCAPE '\\') AND (:kind < 0 OR " + SAVED_KIND_SQL + " = :kind)")
    suspend fun filteredIds(album: String?, query: String, kind: Int): List<Long>
    @Query("SELECT COUNT(*) FROM saved_images")
    fun observeCount(): Flow<Int>
    @Query("SELECT " + SAVED_KIND_SQL + " AS kind, COUNT(*) AS total FROM saved_images GROUP BY kind")
    fun observeKindCounts(): Flow<List<SavedKindCount>>
    @Query("SELECT album AS album, COUNT(*) AS total FROM saved_images GROUP BY album ORDER BY album ASC")
    fun observeAlbums(): Flow<List<AlbumCount>>
    @Query("SELECT * FROM saved_images WHERE id = :id")
    suspend fun getById(id: Long): SavedImageEntity?
    @Query("SELECT * FROM saved_images WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SavedImageEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SavedImageEntity): Long
    @Query("DELETE FROM saved_images WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("DELETE FROM saved_images WHERE id IN (:ids)")
    suspend fun deleteMany(ids: List<Long>)
    @Query("UPDATE saved_images SET uri = :uri, displayName = :name, width = :width, height = :height, createdAt = :createdAt WHERE id = :id")
    suspend fun updateUri(id: Long, uri: String, name: String, width: Int, height: Int, createdAt: Long)
}

@Dao
interface FrameEditDao {
    @Query("SELECT * FROM frame_edits WHERE frameId = :frameId")
    suspend fun get(frameId: Long): FrameEditEntity?
    @Query("SELECT e.* FROM frame_edits e INNER JOIN frames f ON f.id = e.frameId WHERE f.projectId = :projectId")
    fun observeProject(projectId: Long): Flow<List<FrameEditEntity>>
    @Query("SELECT * FROM frame_edits WHERE frameId IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<FrameEditEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FrameEditEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMany(entities: List<FrameEditEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistoryIgnore(entities: List<FrameEditHistoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryAll(entities: List<FrameEditHistoryEntity>)
    @Query("DELETE FROM frame_edit_history WHERE frameId IN (:ids) AND revision > COALESCE((SELECT e.revision FROM frame_edits e WHERE e.frameId = frame_edit_history.frameId), 0)")
    suspend fun deleteFutureMany(ids: List<Long>)
    @Query("DELETE FROM frame_edits WHERE frameId = :frameId")
    suspend fun delete(frameId: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: FrameEditHistoryEntity)
    @Query("SELECT * FROM frame_edit_history WHERE frameId = :frameId AND revision = :revision LIMIT 1")
    suspend fun historyAt(frameId: Long, revision: Int): FrameEditHistoryEntity?
    @Query("SELECT * FROM frame_edit_history WHERE frameId = :frameId AND revision < :revision ORDER BY revision DESC LIMIT 1")
    suspend fun previous(frameId: Long, revision: Int): FrameEditHistoryEntity?
    @Query("SELECT * FROM frame_edit_history WHERE frameId = :frameId AND revision > :revision ORDER BY revision ASC LIMIT 1")
    suspend fun next(frameId: Long, revision: Int): FrameEditHistoryEntity?
    @Query("DELETE FROM frame_edit_history WHERE frameId = :frameId AND revision > :revision")
    suspend fun deleteFuture(frameId: Long, revision: Int)
}

@Dao
interface VideoIndexDao {
    @Query("SELECT COUNT(*) FROM video_index WHERE projectId = :projectId")
    suspend fun count(projectId: Long): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<VideoIndexEntity>)
    @Query("DELETE FROM video_index WHERE projectId = :projectId")
    suspend fun clearIndex(projectId: Long)
    @Query("INSERT INTO video_index(projectId, sampleIndex, ptsUs, isKeyframe, nearestKeyframeIndex, nearestKeyframePtsUs) SELECT :projectId, sampleIndex, ptsUs, isKeyframe, nearestKeyframeIndex, nearestKeyframePtsUs FROM video_index WHERE projectId = :stagingId")
    suspend fun promoteIndex(stagingId: Long, projectId: Long)
    @Query("SELECT * FROM video_index WHERE projectId = :projectId AND sampleIndex = :sampleIndex LIMIT 1")
    suspend fun bySampleIndex(projectId: Long, sampleIndex: Int): VideoIndexEntity?
    @Query("SELECT * FROM video_index WHERE projectId = :projectId AND sampleIndex <= :sampleIndex AND isKeyframe = 1 ORDER BY sampleIndex DESC LIMIT 1")
    suspend fun nearestKeyframe(projectId: Long, sampleIndex: Int): VideoIndexEntity?
    @Query("SELECT * FROM video_index WHERE projectId = :projectId ORDER BY sampleIndex ASC LIMIT :limit OFFSET :offset")
    suspend fun page(projectId: Long, limit: Int, offset: Int): List<VideoIndexEntity>
    @Query("SELECT * FROM video_index WHERE projectId = :projectId AND sampleIndex > :after ORDER BY sampleIndex ASC LIMIT :limit")
    suspend fun pageAfter(projectId: Long, after: Int, limit: Int): List<VideoIndexEntity>
    @Query("SELECT * FROM video_index WHERE projectId = :projectId AND ptsUs >= :startUs AND ptsUs < :endUs ORDER BY sampleIndex ASC LIMIT :limit OFFSET :offset")
    suspend fun pageRange(projectId: Long, startUs: Long, endUs: Long, limit: Int, offset: Int): List<VideoIndexEntity>
    @Query("SELECT * FROM video_index WHERE projectId = :projectId AND sampleIndex > :after AND ptsUs >= :startUs AND ptsUs < :endUs ORDER BY sampleIndex ASC LIMIT :limit")
    suspend fun pageRangeAfter(projectId: Long, startUs: Long, endUs: Long, after: Int, limit: Int): List<VideoIndexEntity>
    @Query("SELECT COUNT(*) FROM video_index WHERE projectId = :projectId AND ptsUs >= :startUs AND ptsUs < :endUs")
    suspend fun countRange(projectId: Long, startUs: Long, endUs: Long): Int
    @Query("SELECT * FROM video_index WHERE projectId = :projectId AND ptsUs >= :startUs AND ptsUs < :endUs ORDER BY ptsUs ASC LIMIT 1 OFFSET :offset")
    suspend fun rangeOffset(projectId: Long, startUs: Long, endUs: Long, offset: Int): VideoIndexEntity?
    @Query("SELECT * FROM video_index WHERE projectId = :projectId AND ptsUs >= :startUs AND ptsUs < :endUs AND ptsUs >= :minPts ORDER BY ptsUs ASC, sampleIndex ASC LIMIT 1")
    suspend fun firstAtOrAfter(projectId: Long, startUs: Long, endUs: Long, minPts: Long): VideoIndexEntity?
    @Query("SELECT * FROM video_index WHERE projectId = :projectId AND sampleIndex > :after AND ptsUs >= :startUs AND ptsUs < :endUs AND ptsUs >= :minPts ORDER BY ptsUs ASC, sampleIndex ASC LIMIT 1")
    suspend fun firstAtOrAfterAfter(projectId: Long, startUs: Long, endUs: Long, minPts: Long, after: Int): VideoIndexEntity?
    @Query("SELECT * FROM video_index WHERE projectId = :projectId AND ptsUs <= :targetUs ORDER BY ptsUs DESC, sampleIndex DESC LIMIT 1")
    suspend fun atOrBefore(projectId: Long, targetUs: Long): VideoIndexEntity?
    @Query("SELECT * FROM video_index WHERE projectId = :projectId AND ptsUs >= :targetUs ORDER BY ptsUs ASC, sampleIndex ASC LIMIT 1")
    suspend fun atOrAfter(projectId: Long, targetUs: Long): VideoIndexEntity?
    @Query("SELECT * FROM video_index WHERE projectId = :projectId ORDER BY sampleIndex ASC")
    suspend fun list(projectId: Long): List<VideoIndexEntity>
}
