package com.frameout.core.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>
    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeById(id: Long): Flow<ProjectEntity?>
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): ProjectEntity?
    @Query("SELECT * FROM projects WHERE id = :id")
    fun getByIdSync(id: Long): ProjectEntity?
    @Query("SELECT * FROM projects WHERE status IN (:statuses) ORDER BY id ASC")
    suspend fun getByStatuses(statuses: List<String>): List<ProjectEntity>
    @Query("SELECT * FROM projects WHERE sourceUri = :sourceUri ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getBySourceUri(sourceUri: String): ProjectEntity?
    @Query("SELECT id FROM projects WHERE name = :name AND sourceName = :sourceName AND createdAt = :createdAt AND durationMs = :durationMs AND frameCount = :frameCount AND lastPtsUs = :lastPtsUs AND status = 'Completed' LIMIT 1")
    suspend fun findImported(name: String, sourceName: String, createdAt: Long, durationMs: Long, frameCount: Int, lastPtsUs: Long): Long?
    @Query("SELECT sourceUri FROM projects")
    suspend fun allSourceUris(): List<String>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProjectEntity): Long
    @Update
    suspend fun update(entity: ProjectEntity)
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("UPDATE projects SET status = :status, extractedCount = :extracted, lastPtsUs = :pts, updatedAt = :updated WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, extracted: Int, pts: Long, updated: Long)
    @Query("UPDATE projects SET status = :status, extractedCount = :extracted, lastPtsUs = :pts, lastSampleIndex = :sampleIndex, updatedAt = :updated WHERE id = :id")
    suspend fun updateCheckpoint(id: Long, status: String, extracted: Int, pts: Long, sampleIndex: Int, updated: Long)
    @Query("UPDATE projects SET targetCount = :target WHERE id = :id")
    suspend fun setTargetCount(id: Long, target: Int)
    @Query("UPDATE projects SET name = :name, updatedAt = :updated WHERE id = :id")
    suspend fun rename(id: Long, name: String, updated: Long)
    @Query("UPDATE projects SET durationMs = :durationMs, frameCount = :frameCount, extractMode = :mode, autoSave = :autoSave, saveFormat = :saveFormat, jpegQuality = :jpegQuality, startMs = :startMs, endMs = :endMs, updatedAt = :updated WHERE id = :id")
    suspend fun updateIndexMeta(id: Long, durationMs: Long, frameCount: Int, mode: String, autoSave: Boolean, saveFormat: String, jpegQuality: Int, startMs: Long, endMs: Long, updated: Long)
    @Query("UPDATE projects SET gateFlags = :flags WHERE id = :id")
    suspend fun setGateFlags(id: Long, flags: Int)
    @Query("UPDATE projects SET status = :status, updatedAt = :updated WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, updated: Long)
    @Query("UPDATE projects SET thumbnailPath = :path, updatedAt = :updated WHERE id = :id")
    suspend fun setThumbnail(id: Long, path: String?, updated: Long)
}
