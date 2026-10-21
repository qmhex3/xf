package com.frameout.app.frames

import com.frameout.core.db.FrameDao
import com.frameout.core.db.FrameTagDao
import com.frameout.core.quality.FrameImportanceAnalyzer
import com.frameout.core.quality.DuplicateFinder
import com.frameout.core.quality.FrameSharpnessAnalyzer
import com.frameout.core.quality.PeopleScan
import com.frameout.core.util.QualityStrings

internal suspend fun sharpestSelection(dao: FrameDao, projectId: Long): Pair<Set<Long>, String> {
    val ids = runCatching { FrameSharpnessAnalyzer.bestIds(dao, projectId) }.getOrDefault(emptyList())
    val text = if (ids.isEmpty()) QualityStrings.noThumbnails() else QualityStrings.sharpestPicked(ids.size)
    return ids.toSet() to text
}

internal suspend fun duplicateSelection(dao: FrameDao, projectId: Long): Pair<Set<Long>, String> {
    val ids = runCatching { DuplicateFinder.duplicateIds(dao, projectId) }.getOrDefault(emptyList())
    val text = if (ids.isEmpty()) QualityStrings.noDuplicates() else QualityStrings.duplicatesPicked(ids.size)
    return ids.toSet() to text
}

internal suspend fun noPeopleSelection(dao: FrameDao, projectId: Long): Pair<Set<Long>, String> {
    val ids = runCatching { PeopleScan.withoutPeople(dao, projectId) }.getOrDefault(emptyList())
    val text = if (ids.isEmpty()) QualityStrings.noPeopleFound() else QualityStrings.noPeoplePicked(ids.size)
    return ids.toSet() to text
}

internal suspend fun importantSelection(frames: FrameDao, tags: FrameTagDao, projectId: Long): Pair<Set<Long>, String> {
    val ids = runCatching { FrameImportanceAnalyzer.bestIds(frames, tags, projectId) }.getOrDefault(emptyList())
    val text = if (ids.isEmpty()) QualityStrings.noThumbnails() else QualityStrings.importantPicked(ids.size)
    return ids.toSet() to text
}
