package com.frameout.engine.extract

import com.frameout.core.util.SmoothScale
import android.graphics.Bitmap
import com.frameout.core.db.FrameEntity
import com.frameout.core.storage.StorageManager
import com.frameout.core.storage.ThumbSpec
import com.frameout.engine.index.SampleInfo

internal data class SampleResult(val frame: FrameEntity?, val pts: Long, val histogram: FloatArray?)

internal class SampleWriter(private val storage: StorageManager) {
    suspend fun write(request: ExtractRequest, sample: SampleInfo, pts: Long, bitmap: Bitmap, histogram: FloatArray?): SampleResult {
        var thumbPath: String? = null
        var committed = false
        try {
            val thumb = scaleThumb(bitmap)
            thumbPath = try {
                storage.writeThumb(request.projectId, sample.index, thumb)
            } finally {
                if (thumb !== bitmap) thumb.recycle()
            }
            val fullPath = if (request.autoSave) storage.writeFrame(request.projectId, sample.index, bitmap, request.saveFormat, request.jpegQuality) else null
            AnalysisCopy.write(storage, request, sample.index, bitmap)
            committed = true
            return SampleResult(FrameEntity(projectId = request.projectId, index = sample.index, ptsUs = pts, isKeyframe = sample.isKeyframe, thumbnailPath = thumbPath, fullPath = fullPath), pts, histogram)
        } finally {
            if (!committed) storage.deleteQuietly(thumbPath)
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun scaleThumb(src: Bitmap): Bitmap {
        val max = ThumbSpec.MAX_SIDE
        if (src.width <= max && src.height <= max) return src.copy(src.config ?: Bitmap.Config.ARGB_8888, false)
        val scale = max.toFloat() / maxOf(src.width, src.height)
        return SmoothScale.scaled(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true)
    }
}
