package com.frameout.engine.edit

import android.content.Context
import java.io.File

@androidx.media3.common.util.UnstableApi
class ClipTranscoder(private val context: Context) {
    suspend fun trim(sourceUri: String, startMs: Long, endMs: Long, out: File): ReassembleResult {
        if (endMs <= startMs) return ReassembleResult.Failure("Empty range")
        out.parentFile?.mkdirs()
        if (out.exists()) out.delete()
        val plan = ReassemblePlan(sourceUri, out.absolutePath, listOf(KeptRange(startMs * 1000L, endMs * 1000L)))
        return TransformerReassembler(context).reencode(plan, out, optimizeTrim = true)
    }
}
