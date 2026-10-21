package com.frameout.engine.extract

import androidx.room3.withWriteTransaction
import com.frameout.core.db.AppDatabase
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.VideoIndexEntity
import com.frameout.core.model.ProjectStatus

class CheckpointStore(private val db: AppDatabase) {
    suspend fun commit(projectId: Long, status: ProjectStatus, done: Int, pts: Long, frames: List<FrameEntity>) {
        commit(projectId, status, done, pts, frames.lastOrNull()?.index ?: -1, frames)
    }

    suspend fun commit(projectId: Long, status: ProjectStatus, done: Int, pts: Long, sampleIndex: Int, frames: List<FrameEntity> = emptyList(), videoIndex: List<VideoIndexEntity> = emptyList()) {
        db.withWriteTransaction {
            if (frames.isNotEmpty()) db.frameDao().insertAll(frames)
            if (videoIndex.isNotEmpty()) db.videoIndexDao().insertAll(videoIndex)
            db.projectDao().updateCheckpoint(projectId, status.name, done, pts.coerceAtLeast(0L), sampleIndex, System.currentTimeMillis())
        }
    }
}
