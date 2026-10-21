package com.frameout.core.edit

import com.frameout.core.db.FrameEditEntity

fun FrameEditEntity.toEdit() = FrameEdit(
    frameId = frameId,
    rotation = rotation,
    flipH = flipH,
    flipV = flipV,
    cropL = cropL,
    cropT = cropT,
    cropR = cropR,
    cropB = cropB,
    brightness = brightness,
    contrast = contrast,
    saturation = saturation,
    marks = MarkCodec.decode(marks),
    filter = filter,
    filterStrength = filterStrength
)

fun FrameEdit.toEntity() = FrameEditEntity(
    frameId = frameId,
    rotation = rotation,
    flipH = flipH,
    flipV = flipV,
    cropL = cropL,
    cropT = cropT,
    cropR = cropR,
    cropB = cropB,
    brightness = brightness,
    contrast = contrast,
    saturation = saturation,
    marks = MarkCodec.encode(marks),
    filter = filter,
    filterStrength = filterStrength
)
