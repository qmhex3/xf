package com.frameout.core.db

import com.frameout.core.quality.NormBox

data class CoverLane(val kind: String?, val frames: List<FrameEntity>)

data class AlbumCover(
    val frame: FrameEntity,
    val focus: NormBox?,
    val behind: List<FrameEntity> = emptyList(),
    val lanes: List<CoverLane> = emptyList()
)
