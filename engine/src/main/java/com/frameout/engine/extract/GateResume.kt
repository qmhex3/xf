package com.frameout.engine.extract

import android.graphics.BitmapFactory
import com.frameout.core.db.FrameDao
import com.frameout.core.quality.BlockLedger
import com.frameout.core.quality.ExtractQualityGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object GateResume {
    suspend fun gate(request: ExtractRequest, frameDao: FrameDao, resumed: Boolean, blockAware: Boolean = true): ExtractQualityGate? {
        val gate = request.newGate(blockAware) ?: return null
        if (resumed) seed(gate, frameDao, request.projectId, blockAware)
        return gate
    }

    private suspend fun seed(gate: ExtractQualityGate, frameDao: FrameDao, projectId: Long, blockAware: Boolean) = withContext(Dispatchers.IO) {
        val blocks = BlockLedger.Lookup()
        val frames = runCatching { frameDao.lastFrames(projectId, ExtractQualityGate.HISTORY) }.getOrNull().orEmpty()
        for (frame in frames.asReversed()) {
            val bitmap = frame.thumbnailPath?.let { path -> runCatching { BitmapFactory.decodeFile(path) }.getOrNull() } ?: continue
            try {
                gate.seed(bitmap, if (blockAware) blocks.factor(frame.thumbnailPath, frame.index) else 1.0)
            } finally {
                bitmap.recycle()
            }
        }
    }
}
