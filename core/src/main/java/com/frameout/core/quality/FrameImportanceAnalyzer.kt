package com.frameout.core.quality

import com.frameout.core.db.FrameDao
import com.frameout.core.db.FrameTagDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FrameImportanceAnalyzer {
    suspend fun bestIds(frames: FrameDao, tags: FrameTagDao, projectId: Long): List<Long> {
        val scored = FrameSharpnessAnalyzer.scoreProject(frames, projectId)
        if (scored.isEmpty()) return emptyList()
        val byFrame = tags.allTags(projectId).groupBy({ it.frameId }) { TagHit(it.tag, it.confidence, NormBoxCodec.decode(it.boxes)) }
        return withContext(Dispatchers.Default) { FrameImportance.pick(scored, byFrame) }
    }
}
