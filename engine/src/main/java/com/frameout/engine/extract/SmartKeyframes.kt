package com.frameout.engine.extract

import android.content.Context
import com.frameout.core.db.AppDatabase
import com.frameout.core.pro.ProAccess
import com.frameout.core.quality.FrameImportance
import com.frameout.core.quality.FrameImportanceAnalyzer
import com.frameout.core.quality.FrameTagScanner
import com.frameout.core.quality.ScanVerdict
import com.frameout.core.util.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

internal object SmartKeyframes {
    private const val FILE = "frameout_smart_keyframes"
    private const val BUDGET_MS = 4 * 60_000L
    private const val MAX_FRAMES = 600
    private const val CHUNK = 500

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun key(projectId: Long) = "done_$projectId"

    fun reset(context: Context, projectId: Long) {
        prefs(context).edit().remove(key(projectId)).apply()
    }

    suspend fun refine(context: Context, projectId: Long) {
        try {
            val db = AppDatabase.get(context)
            val project = db.projectDao().getByIdSync(projectId) ?: return
            if (!ProAccess.isProActive() || project.autoSave) return
            if (ExtractModeCodec.decode(project.extractMode) != ExtractMode.KeyframesOnly) return
            if (prefs(context).getBoolean(key(projectId), false)) return

            val frames = db.frameDao()
            val tags = db.frameTagDao()
            val active = frames.listActiveIds(projectId)
            if (active.size < FrameImportance.MIN_FRAMES || active.size > MAX_FRAMES) return

            val keep = withTimeoutOrNull(BUDGET_MS) {
                val report = FrameTagScanner.scan(tags, projectId) { }
                if (report.verdict != ScanVerdict.Complete) emptyList() else FrameImportanceAnalyzer.bestIds(frames, tags, projectId)
            }?.toSet().orEmpty()
            if (keep.isEmpty()) return

            active.filter { it !in keep }.chunked(CHUNK).forEach { frames.softDelete(it) }
            prefs(context).edit().putBoolean(key(projectId), true).apply()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFailure("SmartKeyframes", e)
        }
    }
}
