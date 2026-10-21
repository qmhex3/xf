package com.frameout.engine.edit

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.frameout.core.util.logFailure
import com.frameout.engine.work.ReencodeForeground
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

@androidx.media3.common.util.UnstableApi
internal class TransformerReassembler(private val context: Context) {
    suspend fun reencode(plan: ReassemblePlan, out: File, optimizeTrim: Boolean = false): ReassembleResult {
        val result = try {
            withTimeoutOrNull(TIMEOUT_MS) { ReencodeForeground.around(context) { run(plan, out, optimizeTrim) } }
        } catch (e: CancellationException) {
            out.delete()
            throw e
        }
        if (result == null) {
            out.delete()
            return ReassembleResult.Failure("Reassembly timed out")
        }
        if (result !is ReassembleResult.Success) out.delete()
        return result
    }

    private suspend fun run(plan: ReassemblePlan, out: File, optimizeTrim: Boolean): ReassembleResult = suspendCancellableCoroutine { cont ->
        val main = Handler(Looper.getMainLooper())
        var transformer: Transformer? = null
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, result: ExportResult) {
                val outcome = if (out.isFile && out.length() > 0L) ReassembleResult.Success(out.absolutePath, out.length()) else ReassembleResult.Failure("Empty output")
                if (cont.isActive) cont.resume(outcome)
            }

            override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                logFailure("TransformerReassembler", exception)
                if (cont.isActive) cont.resume(ReassembleResult.Failure("Reassembly failed: ${exception.errorCodeName}"))
            }
        }
        cont.invokeOnCancellation {
            main.post {
                runCatching { transformer?.cancel() }
                out.delete()
            }
        }
        main.post {
            if (!cont.isActive) return@post
            try {
                transformer = startTransformer(plan, listener, out, optimizeTrim)
            } catch (e: Exception) {
                logFailure("TransformerReassembler", e)
                if (cont.isActive) cont.resume(ReassembleResult.Failure("Reassembly failed to start"))
            }
        }
    }

    private fun startTransformer(plan: ReassemblePlan, listener: Transformer.Listener, out: File, optimizeTrim: Boolean): Transformer {
        val items = plan.keptRanges.map { range ->
            val media = MediaItem.Builder().setUri(Uri.parse(plan.sourceUri)).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(range.startUs / 1000L)
                    .setEndPositionMs(range.endUs / 1000L)
                    .build()
            ).build()
            EditedMediaItem.Builder(media).build()
        }
        val composition = Composition.Builder(EditedMediaItemSequence.withAudioAndVideoFrom(items)).build()
        val transformer = Transformer.Builder(context)
            .setLooper(Looper.getMainLooper())
            .addListener(listener)
            .experimentalSetTrimOptimizationEnabled(optimizeTrim)
            .build()
        transformer.start(composition, out.absolutePath)
        return transformer
    }

    private companion object {
        const val TIMEOUT_MS = 30L * 60L * 1000L
    }
}
