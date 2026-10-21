package com.frameout.app.frames

import com.frameout.core.edit.EditHistoryRepository
import com.frameout.core.edit.FrameEdit

suspend fun applyEditsToIds(
    history: EditHistoryRepository,
    ids: List<Long>,
    transform: (FrameEdit) -> FrameEdit
) {
    history.transformMany(ids, transform)
}
