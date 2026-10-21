package com.frameout.engine.edit

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@androidx.media3.common.util.UnstableApi
class VideoReassembler(private val context: Context) {
    private val copier = VideoSampleCopier(context)
    private val transformer = TransformerReassembler(context)

    suspend fun reassemble(plan: ReassemblePlan): ReassembleResult {
        if (plan.isEmpty) return ReassembleResult.Failure("No frames kept")
        val out = File(plan.outputPath).prepareOutput()
        if (withContext(Dispatchers.IO) { copier.copy(plan, out) }) return ReassembleResult.Success(out.absolutePath, out.length(), false)
        out.delete()
        return when (val result = transformer.reencode(plan, out)) {
            is ReassembleResult.Success -> result.copy(reencoded = true)
            is ReassembleResult.Failure -> result
        }
    }

    private fun File.prepareOutput(): File {
        parentFile?.mkdirs()
        if (exists()) delete()
        return this
    }
}
