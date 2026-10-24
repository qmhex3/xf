package com.frameout.engine.extract

import com.frameout.engine.decode.AnimatedFrameSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object AnimatedSelection {
    fun count(decoder: AnimatedFrameSource, mode: ExtractMode, startMs: Long, endMs: Long, checkpoint: Long, checkpointIndex: Int = -1): Int = sequence(decoder, mode, startMs, endMs, checkpoint, checkpointIndex).count()

    fun flow(decoder: AnimatedFrameSource, mode: ExtractMode, startMs: Long, endMs: Long, checkpoint: Long, checkpointIndex: Int = -1): Flow<Int> = flow {
        sequence(decoder, mode, startMs, endMs, checkpoint, checkpointIndex).forEach { emit(it) }
    }

    private fun sequence(decoder: AnimatedFrameSource, mode: ExtractMode, startMs: Long, endMs: Long, checkpoint: Long, checkpointIndex: Int): Sequence<Int> {
        val total = decoder.frameCount()
        val startUs = startMs.coerceAtLeast(0) * 1000L
        val endUs = if (endMs > 0) endMs * 1000L else Long.MAX_VALUE
        val first = (0 until total).firstOrNull { decoder.ptsUs(it) >= startUs } ?: total
        val last = (first until total).firstOrNull { decoder.ptsUs(it) >= endUs } ?: total
        if (first >= last) return emptySequence()
        fun after(index: Int): Boolean = if (checkpointIndex >= 0) index > checkpointIndex else decoder.ptsUs(index) > checkpoint
        return when (mode) {
            ExtractMode.EveryFrame, ExtractMode.SceneChange, ExtractMode.Slides -> (first until last).asSequence().filter(::after)
            is ExtractMode.EveryNFrames -> (first until last step mode.n.coerceAtLeast(1)).asSequence().filter(::after)
            is ExtractMode.EveryNMillis -> timed(decoder, first, last, mode.millis.coerceAtLeast(1) * 1000L).filter(::after)
            ExtractMode.KeyframesOnly -> timed(decoder, first, last, KEYFRAME_SPAN_US).filter(::after)
            is ExtractMode.Content -> timed(decoder, first, last, com.frameout.core.quality.content.ContentSpec.SAMPLE_STEP_MS * 1000L).filter(::after)
            is ExtractMode.FixedCount -> fixedFromStart(decoder, total, startUs, endUs, mode.count, checkpointIndex).filter(::after)
        }
    }

    private const val KEYFRAME_SPAN_US = 1_000_000L

    private fun timed(decoder: AnimatedFrameSource, start: Int, last: Int, stepUs: Long): Sequence<Int> = sequence {
        var target = decoder.ptsUs(start)
        for (index in start until last) {
            if (decoder.ptsUs(index) >= target) {
                yield(index)
                target = TimeGrid.advance(target, decoder.ptsUs(index), stepUs)
            }
        }
    }

    private fun fixedFromStart(decoder: AnimatedFrameSource, total: Int, startUs: Long, endUs: Long, requested: Int, checkpointIndex: Int): Sequence<Int> {
        val first = (0 until total).firstOrNull { decoder.ptsUs(it) >= startUs } ?: return emptySequence()
        val last = (first until total).firstOrNull { decoder.ptsUs(it) >= endUs } ?: total
        val count = requested.coerceIn(0, (last - first).coerceAtLeast(0))
        return sequence {
            val picks = FixedSelection.positions(last - first, count) { decoder.ptsUs(first + it) }
            for (offset in picks) {
                val index = first + offset
                if (index > checkpointIndex) yield(index)
            }
        }
    }

}
