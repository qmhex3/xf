package com.frameout.engine.extract

import android.graphics.BitmapFactory
import com.frameout.core.db.FrameDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object ResumeReference {
    suspend fun load(frameDao: FrameDao, projectId: Long, mode: ExtractMode): FloatArray? {
        if (mode != ExtractMode.SceneChange && mode != ExtractMode.Slides) return null
        return withContext(Dispatchers.IO) {
            val path = runCatching { frameDao.lastFrame(projectId)?.thumbnailPath }.getOrNull() ?: return@withContext null
            val bitmap = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return@withContext null
            try {
                SceneChangeSelector.signature(mode, bitmap)
            } finally {
                bitmap.recycle()
            }
        }
    }
}
