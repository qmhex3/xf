package com.frameout.core.db

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
abstract class SharpestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(rows: List<SharpestFrameEntity>)
    @Query("DELETE FROM sharpest_frames WHERE projectId = :projectId")
    abstract suspend fun clear(projectId: Long)
    @Query("SELECT f.* FROM frames f INNER JOIN sharpest_frames s ON s.frameId = f.id WHERE s.projectId = :projectId AND f.projectId = :projectId AND f.isDeleted = 0 ORDER BY f.`index` ASC")
    abstract fun paging(projectId: Long): PagingSource<Int, FrameEntity>
    @Query("SELECT COUNT(*) FROM sharpest_frames s INNER JOIN frames f ON s.frameId = f.id WHERE s.projectId = :projectId AND f.projectId = :projectId AND f.isDeleted = 0")
    abstract suspend fun count(projectId: Long): Int
    @Query("SELECT f.id FROM frames f INNER JOIN sharpest_frames s ON s.frameId = f.id WHERE s.projectId = :projectId AND f.projectId = :projectId AND f.isDeleted = 0 ORDER BY f.`index` ASC")
    abstract suspend fun listIds(projectId: Long): List<Long>
    @Query("SELECT f.ptsUs FROM frames f INNER JOIN sharpest_frames s ON s.frameId = f.id WHERE s.projectId = :projectId AND f.projectId = :projectId AND f.isDeleted = 0 ORDER BY f.`index` ASC LIMIT 1 OFFSET :offset")
    abstract suspend fun ptsAtOffset(projectId: Long, offset: Int): Long?

    @Transaction
    open suspend fun replace(projectId: Long, frameIds: List<Long>) {
        clear(projectId)
        frameIds.distinct().chunked(SQL_IN_CHUNK).forEach { chunk ->
            insertAll(chunk.map { SharpestFrameEntity(projectId, it) })
        }
    }
}
