package com.frameout.core.backup

data class BackupProject(
    val name: String,
    val sourceName: String,
    val sourceUri: String,
    val durationMs: Long,
    val extractMode: String,
    val saveFormat: String,
    val jpegQuality: Int,
    val startMs: Long,
    val endMs: Long,
    val saveSubfolder: String,
    val createdAt: Long
)

data class BackupEdit(
    val rotation: Int,
    val flipH: Boolean,
    val flipV: Boolean,
    val cropL: Float,
    val cropT: Float,
    val cropR: Float,
    val cropB: Float,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val isDeleted: Boolean,
    val marks: String = "",
    val filter: String = "",
    val filterStrength: Float = 1f
)

data class BackupFrame(
    val index: Int,
    val ptsUs: Long,
    val isKeyframe: Boolean,
    val isFavorite: Boolean,
    val isDeleted: Boolean,
    val edit: BackupEdit?
)

data class BackupData(val project: BackupProject, val frames: List<BackupFrame>)

class BackupException(val reason: Reason) : Exception(reason.name) {
    enum class Reason { Invalid, Newer, TooLarge, NoSpace }
}
