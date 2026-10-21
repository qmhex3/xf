package com.frameout.app.frames

import com.frameout.core.db.FrameDao
import com.frameout.core.db.SharpestDao

suspend fun FrameDao.ptsMsAt(projectId: Long, filter: FrameFilter, offset: Int): Long? =
    ptsAtOffset(projectId, filter == FrameFilter.Deleted, filter == FrameFilter.Favorites, offset)?.div(1000)

suspend fun SharpestDao.ptsMsAtSharpest(projectId: Long, offset: Int): Long? =
    ptsAtOffset(projectId, offset)?.div(1000)
