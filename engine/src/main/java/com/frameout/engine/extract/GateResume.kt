package com.frameout.engine.extract

import android.graphics.BitmapFactory
import com.frameout.core.db.FrameDao
import com.frameout.core.quality.ExtractQualityGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object GateResume {
    suspend fun gate(request: ExtractRequest, frameDao: FrameDao, resumed: Boolean): ExtractQualityGate? {
        val gate = request.newGate() ?: return null
        if (resumed) seed(gate, frameDao, request.projectId)
        return gate
    }

    private suspend fun seed(gate: ExtractQualityGate, frameDao: FrameDao, projectId: Long) = withContext(Dispatchers.IO) {
        val frames = runCatching { frameDao.lastFrames(projectId, ExtractQualityGate.HISTORY) }.getOrNull().orEmpty()
        for (frame in frames.asReversed()) {
            val bitmap = frame.thumbnailPath?.let { path -> runCatching { BitmapFactory.decodeFile(path) }.getOrNull() } ?: continue
            try {
                gate.seed(bitmap)
            } finally {
                bitmap.recycle()
            }
        }
    }
}
