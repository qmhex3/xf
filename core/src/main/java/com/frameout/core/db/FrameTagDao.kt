package com.frameout.core.db

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
abstract class FrameTagDao {
    @Insert
    abstract suspend fun insertTags(tags: List<FrameTagEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertScans(scans: List<FrameScanEntity>)
    @Query("SELECT * FROM frames WHERE projectId = :projectId AND isDeleted = 0 AND id NOT IN (SELECT frameId FROM frame_scans) ORDER BY `index` ASC LIMIT :limit")
    abstract suspend fun unscanned(projectId: Long, limit: Int): List<FrameEntity>
    @Query("SELECT COUNT(*) FROM frames WHERE projectId = :projectId AND isDeleted = 0 AND id NOT IN (SELECT frameId FROM frame_scans)")
    abstract suspend fun countUnscanned(projectId: Long): Int
    @Query("SELECT tag AS tag, COUNT(DISTINCT frameId) AS frames FROM frame_tags WHERE projectId = :projectId AND frameId IN (SELECT id FROM frames WHERE isDeleted = 0) GROUP BY tag ORDER BY frames DESC, tag ASC")
    abstract suspend fun tagCounts(projectId: Long): List<TagCount>
    @Query("SELECT * FROM frames WHERE projectId = :projectId AND isDeleted = 0 AND id IN (SELECT frameId FROM frame_tags WHERE projectId = :projectId AND tag IN (:tags) GROUP BY frameId HAVING COUNT(DISTINCT tag) = :n) ORDER BY `index` ASC")
    abstract fun pagingTagged(projectId: Long, tags: List<String>, n: Int): PagingSource<Int, FrameEntity>
    @Query("SELECT COUNT(*) FROM frames WHERE projectId = :projectId AND isDeleted = 0 AND id IN (SELECT frameId FROM frame_tags WHERE projectId = :projectId AND tag IN (:tags) GROUP BY frameId HAVING COUNT(DISTINCT tag) = :n)")
    abstract suspend fun countTagged(projectId: Long, tags: List<String>, n: Int): Int
    @Query("SELECT id FROM frames WHERE projectId = :projectId AND isDeleted = 0 AND id IN (SELECT frameId FROM frame_tags WHERE projectId = :projectId AND tag IN (:tags) GROUP BY frameId HAVING COUNT(DISTINCT tag) = :n) ORDER BY `index` ASC")
    abstract suspend fun listTaggedIds(projectId: Long, tags: List<String>, n: Int): List<Long>
    @Query("SELECT ptsUs FROM frames WHERE projectId = :projectId AND isDeleted = 0 AND id IN (SELECT frameId FROM frame_tags WHERE projectId = :projectId AND tag IN (:tags) GROUP BY frameId HAVING COUNT(DISTINCT tag) = :n) ORDER BY `index` ASC LIMIT 1 OFFSET :offset")
    abstract suspend fun ptsAtOffset(projectId: Long, tags: List<String>, n: Int, offset: Int): Long?
    @Query("SELECT tag AS tag, COUNT(DISTINCT frameId) AS frames FROM frame_tags WHERE projectId = :projectId AND tag NOT IN (:tags) AND frameId IN (SELECT frameId FROM frame_tags WHERE projectId = :projectId AND tag IN (:tags) GROUP BY frameId HAVING COUNT(DISTINCT tag) = :n) AND frameId IN (SELECT id FROM frames WHERE isDeleted = 0) GROUP BY tag ORDER BY frames DESC, tag ASC")
    abstract suspend fun coTagCounts(projectId: Long, tags: List<String>, n: Int): List<TagCount>
    @Query("SELECT f.* FROM frames f INNER JOIN frame_tags t ON t.frameId = f.id WHERE t.projectId = :projectId AND t.tag = :tag AND f.isDeleted = 0 ORDER BY t.confidence DESC, f.`index` ASC LIMIT 1")
    abstract suspend fun coverFor(projectId: Long, tag: String): FrameEntity?
    @Query("SELECT t.* FROM frame_tags t INNER JOIN frames f ON t.frameId = f.id WHERE t.projectId = :projectId AND t.tag = :tag AND t.boxes != '' AND f.isDeleted = 0 ORDER BY t.confidence DESC, f.`index` ASC LIMIT 1")
    abstract suspend fun coverTagFor(projectId: Long, tag: String): FrameTagEntity?
    @Query("SELECT f.* FROM frames f INNER JOIN frame_tags t ON t.frameId = f.id WHERE t.projectId = :projectId AND t.tag = :tag AND f.isDeleted = 0 ORDER BY t.confidence DESC, f.`index` ASC LIMIT :limit")
    abstract suspend fun coversFor(projectId: Long, tag: String, limit: Int): List<FrameEntity>
    @Query("SELECT * FROM frame_tags WHERE frameId IN (:frameIds) AND tag LIKE 'l:%'")
    abstract suspend fun labelsForFrames(frameIds: List<Long>): List<FrameTagEntity>
    @Query("SELECT * FROM frame_tags WHERE projectId = :projectId AND (tag LIKE :labelPrefix || '%' OR tag LIKE :objectPrefix || '%') AND frameId NOT IN (SELECT frameId FROM frame_tags WHERE projectId = :projectId AND tag LIKE :familyPrefix || '%')")
    abstract suspend fun detailRowsWithoutFamily(projectId: Long, labelPrefix: String, objectPrefix: String, familyPrefix: String): List<FrameTagEntity>
    @Query("SELECT * FROM frame_tags WHERE frameId = :frameId AND tag IN (:tags) AND boxes != ''")
    abstract suspend fun boxesFor(frameId: Long, tags: List<String>): List<FrameTagEntity>
    @Query("SELECT * FROM frame_tags WHERE frameId = :frameId AND boxes != '' ORDER BY confidence DESC")
    abstract suspend fun boxedTagsForFrame(frameId: Long): List<FrameTagEntity>
    @Query("SELECT t.* FROM frame_tags t INNER JOIN frames f ON t.frameId = f.id WHERE f.projectId = :projectId AND f.isDeleted = 0 AND f.`index` BETWEEN :from AND :to AND f.id != :frameId")
    abstract suspend fun tagsAround(projectId: Long, frameId: Long, from: Int, to: Int): List<FrameTagEntity>
    @Query("SELECT t.* FROM frame_tags t INNER JOIN frames f ON t.frameId = f.id WHERE t.projectId = :projectId AND f.isDeleted = 0")
    abstract suspend fun allTags(projectId: Long): List<FrameTagEntity>
    @Query("DELETE FROM frame_tags WHERE projectId = :projectId")
    abstract suspend fun clearTags(projectId: Long)
    @Query("DELETE FROM frame_scans WHERE projectId = :projectId")
    abstract suspend fun clearScans(projectId: Long)
    @Query("DELETE FROM frame_scans")
    abstract suspend fun clearAllScans()
    @Query("DELETE FROM frame_tags WHERE frameId IN (:frameIds)")
    abstract suspend fun deleteTagsForFrames(frameIds: List<Long>)

    @Transaction
    open suspend fun commitScan(frameIds: List<Long>, tags: List<FrameTagEntity>, scans: List<FrameScanEntity>) {
        if (frameIds.isEmpty()) return
        deleteTagsForFrames(frameIds)
        if (tags.isNotEmpty()) insertTags(tags)
        if (scans.isNotEmpty()) insertScans(scans)
    }
}
