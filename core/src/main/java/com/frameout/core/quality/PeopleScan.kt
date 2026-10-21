package com.frameout.core.quality

import com.frameout.core.db.FrameDao

object PeopleScan {
    suspend fun withoutPeople(dao: FrameDao, projectId: Long): List<Long> =
        FrameScan.mapThumbnails(dao, projectId) { frame, bitmap ->
            frame.id.takeUnless { PeopleFilter.containsPerson(bitmap) }
        }.filterNotNull()
}
