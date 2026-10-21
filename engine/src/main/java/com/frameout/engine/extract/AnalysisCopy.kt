package com.frameout.engine.extract

import android.graphics.Bitmap
import com.frameout.core.quality.AnalysisScale
import com.frameout.core.pro.ProAccess
import com.frameout.core.quality.AnalysisSource
import com.frameout.core.storage.StorageManager
import com.frameout.core.storage.ThumbSpec
import com.frameout.core.util.SmoothScale
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException

internal object AnalysisCopy {
    suspend fun write(storage: StorageManager, request: ExtractRequest, index: Int, bitmap: Bitmap) {
        if (!ProAccess.isProActive()) return
        if (request.autoSave || bitmap.isRecycled || !AnalysisSource.needsCopy(bitmap.width, bitmap.height)) return
        val (width, height) = AnalysisScale.target(bitmap.width, bitmap.height, ThumbSpec.ANALYSIS_SIDE)
        val copy = if (width == bitmap.width && height == bitmap.height) bitmap else SmoothScale.scaled(bitmap, width, height, true)
        try {
            storage.writeAnalysis(request.projectId, index, copy)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFailure("AnalysisCopy", e)
        } finally {
            if (copy !== bitmap) copy.recycle()
        }
    }
}
