package com.frameout.core.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceUri: String,
    val sourceName: String,
    val durationMs: Long = 0,
    val frameCount: Int = 0,
    val extractedCount: Int = 0,
    val lastPtsUs: Long = 0,
    val lastSampleIndex: Int = -1,
    val status: String = "Idle",
    val createdAt: Long,
    val updatedAt: Long,
    val thumbnailPath: String? = null,
    val extractMode: String = "EveryFrame",
    val autoSave: Boolean = false,
    val saveFormat: String = "jpg",
    val jpegQuality: Int = 92,
    val startMs: Long = 0,
    val endMs: Long = 0,
    val saveSubfolder: String = "",
    val targetCount: Int = 0,
    val gateFlags: Int = 0
)

@Entity(
    tableName = "frames",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("projectId"),
        Index(value = ["projectId", "index"], unique = true),
        Index(value = ["projectId", "isDeleted", "index"]),
        Index(value = ["projectId", "isFavorite", "isDeleted", "index"])
    ]
)
data class FrameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val index: Int,
    val ptsUs: Long,
    val isKeyframe: Boolean = false,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val thumbnailPath: String? = null,
    val fullPath: String? = null
)

@Entity(tableName = "saved_images", indices = [Index(value = ["album", "createdAt"])])
data class SavedImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val displayName: String,
    val album: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val createdAt: Long,
    val sourceFrameId: Long? = null
)
