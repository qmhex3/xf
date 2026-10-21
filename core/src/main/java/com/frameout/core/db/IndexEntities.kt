package com.frameout.core.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "video_index",
    indices = [
        Index(value = ["projectId", "sampleIndex"], unique = true),
        Index(value = ["projectId", "ptsUs", "sampleIndex"])
    ]
)
data class VideoIndexEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val sampleIndex: Int,
    val ptsUs: Long,
    val isKeyframe: Boolean,
    val nearestKeyframeIndex: Int,
    val nearestKeyframePtsUs: Long = -1L
)

@Entity(
    tableName = "frame_tags",
    foreignKeys = [
        ForeignKey(
            entity = FrameEntity::class,
            parentColumns = ["id"],
            childColumns = ["frameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("frameId"), Index(value = ["projectId", "tag"])]
)
data class FrameTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val frameId: Long,
    val projectId: Long,
    val tag: String,
    val confidence: Float,
    val boxes: String = ""
)

@Entity(
    tableName = "frame_scans",
    foreignKeys = [
        ForeignKey(
            entity = FrameEntity::class,
            parentColumns = ["id"],
            childColumns = ["frameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class FrameScanEntity(
    @PrimaryKey val frameId: Long,
    val projectId: Long
)

data class TagCount(val tag: String, val frames: Int)

@Entity(
    tableName = "sharpest_frames",
    primaryKeys = ["projectId", "frameId"],
    foreignKeys = [
        ForeignKey(
            entity = FrameEntity::class,
            parentColumns = ["id"],
            childColumns = ["frameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("frameId")]
)
data class SharpestFrameEntity(
    val projectId: Long,
    val frameId: Long
)
