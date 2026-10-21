package com.frameout.core.backup

import com.frameout.core.db.FrameEditEntity
import com.frameout.core.db.FrameEditHistoryEntity
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectEntity

internal fun ProjectEntity.toBackup() = BackupProject(name, sourceName, sourceUri, durationMs, extractMode, saveFormat, jpegQuality, startMs, endMs, saveSubfolder, createdAt)

internal fun FrameEditEntity.toBackup() = BackupEdit(rotation, flipH, flipV, cropL, cropT, cropR, cropB, brightness, contrast, saturation, isDeleted, marks, filter, filterStrength)

internal fun FrameEntity.toBackup(edit: FrameEditEntity?) = BackupFrame(index, ptsUs, isKeyframe, isFavorite, isDeleted, edit?.toBackup())

internal fun BackupEdit.toEntity(frameId: Long) = FrameEditEntity(frameId, rotation, flipH, flipV, cropL, cropT, cropR, cropB, brightness, contrast, saturation, 0, isDeleted, marks, filter, filterStrength)

internal fun BackupEdit.toHistory(frameId: Long) = FrameEditHistoryEntity(
    frameId = frameId, revision = 0, rotation = rotation, flipH = flipH, flipV = flipV,
    cropL = cropL, cropT = cropT, cropR = cropR, cropB = cropB, brightness = brightness,
    contrast = contrast, saturation = saturation, isDeleted = isDeleted, marks = marks,
    filter = filter, filterStrength = filterStrength
)
