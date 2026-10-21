package com.frameout.core.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "frame_edits",
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
data class FrameEditEntity(
    @PrimaryKey val frameId: Long,
    val rotation: Int = 0,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val cropL: Float = 0f,
    val cropT: Float = 0f,
    val cropR: Float = 1f,
    val cropB: Float = 1f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val revision: Int = 0,
    val isDeleted: Boolean = false,
    val marks: String = "",
    val filter: String = "",
    val filterStrength: Float = 1f
)

@Entity(
    tableName = "frame_edit_history",
    foreignKeys = [
        ForeignKey(
            entity = FrameEntity::class,
            parentColumns = ["id"],
            childColumns = ["frameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["frameId", "revision"], unique = true)]
)
data class FrameEditHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val frameId: Long,
    val revision: Int,
    val rotation: Int = 0,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val cropL: Float = 0f,
    val cropT: Float = 0f,
    val cropR: Float = 1f,
    val cropB: Float = 1f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val isDeleted: Boolean = false,
    val marks: String = "",
    val filter: String = "",
    val filterStrength: Float = 1f
)
