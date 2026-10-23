package com.frameout.engine.extract

import com.frameout.core.db.VideoIndexDao
import com.frameout.core.db.VideoIndexEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class IndexSampleSource(
    private val dao: VideoIndexDao,
    private val projectId: Long,
    private val durationUs: Long
) {
    private val pageSize = 500

    suspend fun selectedCount(request: ExtractRequest, checkpoint: Long, checkpointIndex: Int = -1): Int =
        selectedCount(request.mode, bounds(request), checkpoint, checkpointIndex)

    fun sampleFlow(request: ExtractRequest, checkpoint: Long, checkpointIndex: Int = -1): Flow<com.frameout.engine.index.SampleInfo> =
        sampleFlow(request.mode, bounds(request), checkpoint, checkpointIndex)

    internal suspend fun selectedCount(mode: ExtractMode, bounds: Bounds, checkpoint: Long, checkpointIndex: Int): Int {
        if (mode is ExtractMode.FixedCount && checkpoint < 0L && checkpointIndex < 0) {
            return FixedSelection.count(dao, projectId, bounds.startUs, bounds.endUs, mode.count.coerceAtLeast(1))
        }
        if (mode == ExtractMode.EveryFrame && checkpoint < 0L && checkpointIndex < 0) {
            return dao.countRange(projectId, bounds.startUs, bounds.endUs)
        }
        var count = 0
        sampleFlow(mode, bounds, checkpoint, checkpointIndex).collect { count++ }
        return count
    }

    internal fun sampleFlow(mode: ExtractMode, bounds: Bounds, checkpoint: Long, checkpointIndex: Int): Flow<com.frameout.engine.index.SampleInfo> = flow {
        when (mode) {
            ExtractMode.EveryFrame -> pageMode(bounds, checkpoint, checkpointIndex) { true }.collect { emit(it) }
            is ExtractMode.EveryNFrames -> pageMode(bounds, checkpoint, checkpointIndex) { (it.position - it.basePosition) % mode.n.coerceAtLeast(1) == 0 }.collect { emit(it) }
            ExtractMode.KeyframesOnly -> pageMode(bounds, checkpoint, checkpointIndex) { it.sample.isKeyframe }.collect { emit(it) }
            is ExtractMode.EveryNMillis -> timeMode(bounds, checkpoint, checkpointIndex, mode.millis.coerceAtLeast(1) * 1000L).collect { emit(it) }
            is ExtractMode.FixedCount -> fixedMode(bounds, checkpoint, checkpointIndex, mode.count.coerceAtLeast(1)).collect { emit(it) }
            ExtractMode.SceneChange -> sceneMode(bounds, checkpoint, checkpointIndex, SceneChangeSelector.SCENE_STEP_US).collect { emit(it) }
            ExtractMode.Slides -> sceneMode(bounds, checkpoint, checkpointIndex, SceneChangeSelector.SLIDES_STEP_US).collect { emit(it) }
            is ExtractMode.Content -> timeMode(bounds, checkpoint, checkpointIndex, mode.spec.millis.coerceAtLeast(1) * 1000L).collect { emit(it) }
        }
    }

    private fun bounds(request: ExtractRequest): Bounds {
        val start = request.startMs.coerceAtLeast(0) * 1000L
        val end = if (request.endMs > 0) request.endMs * 1000L else durationUs.coerceAtLeast(start + 1L)
        return Bounds(start, maxOf(end, start + 1L))
    }

    private fun pageMode(bounds: Bounds, checkpoint: Long, checkpointIndex: Int, keep: (Positioned) -> Boolean): Flow<com.frameout.engine.index.SampleInfo> = flow {
        var after = checkpointIndex.coerceAtLeast(-1)
        var baseIndex = dao.rangeOffset(projectId, bounds.startUs, bounds.endUs, 0)?.sampleIndex ?: -1
        while (true) {
            val page = if (after >= 0) dao.pageRangeAfter(projectId, bounds.startUs, bounds.endUs, after, pageSize) else dao.pageRange(projectId, bounds.startUs, bounds.endUs, pageSize, 0)
            if (page.isEmpty()) break
            for (entity in page) {
                if (baseIndex < 0) baseIndex = entity.sampleIndex
                val item = Positioned(entity.toSample(), entity.sampleIndex, baseIndex)
                if (keep(item) && afterCheckpoint(entity.sampleIndex, entity.ptsUs, checkpoint, checkpointIndex)) emit(item.sample)
            }
            after = page.last().sampleIndex
            if (page.size < pageSize) break
        }
    }

    private fun timeMode(bounds: Bounds, checkpoint: Long, checkpointIndex: Int, stepUs: Long): Flow<com.frameout.engine.index.SampleInfo> = flow {
        var target = bounds.startUs
        if (checkpoint >= 0) {
            val stepCount = (checkpoint - bounds.startUs).coerceAtLeast(0L) / stepUs + 1L
            target = maxOf(target, safeMultiplyAdd(bounds.startUs, stepCount, stepUs))
        }
        while (target < bounds.endUs) {
            val item = dao.firstAtOrAfter(projectId, bounds.startUs, bounds.endUs, target) ?: break
            if (afterCheckpoint(item.sampleIndex, item.ptsUs, checkpoint, checkpointIndex)) emit(item.toSample())
            target = TimeGrid.advance(target, item.ptsUs, stepUs)
        }
    }

    private fun fixedMode(bounds: Bounds, checkpoint: Long, checkpointIndex: Int, requested: Int): Flow<com.frameout.engine.index.SampleInfo> = flow {
        FixedSelection.flow(dao, projectId, bounds.startUs, bounds.endUs, requested).collect {
            if (afterCheckpoint(it.sampleIndex, it.ptsUs, checkpoint, checkpointIndex)) emit(it.toSample())
        }
    }

    private fun sceneMode(bounds: Bounds, checkpoint: Long, checkpointIndex: Int, stepUs: Long): Flow<com.frameout.engine.index.SampleInfo> = flow {
        var after = -1
        val stepper = CandidateStepper(stepUs)
        while (true) {
            val page = if (after >= 0) dao.pageRangeAfter(projectId, bounds.startUs, bounds.endUs, after, pageSize) else dao.pageRange(projectId, bounds.startUs, bounds.endUs, pageSize, 0)
            if (page.isEmpty()) break
            for (entity in page) {
                if (stepper.accept(entity.ptsUs, entity.isKeyframe) && afterCheckpoint(entity.sampleIndex, entity.ptsUs, checkpoint, checkpointIndex)) emit(entity.toSample())
            }
            after = page.last().sampleIndex
            if (page.size < pageSize) break
        }
    }

    private fun afterCheckpoint(sampleIndex: Int, ptsUs: Long, checkpoint: Long, checkpointIndex: Int): Boolean = if (checkpointIndex >= 0) sampleIndex > checkpointIndex else ptsUs > checkpoint
    private fun safeMultiplyAdd(base: Long, count: Long, step: Long): Long = if (count > 0L && step > 0L && count > (Long.MAX_VALUE - base) / step) Long.MAX_VALUE else base + count * step
    internal data class Bounds(val startUs: Long, val endUs: Long)
    private data class Positioned(val sample: com.frameout.engine.index.SampleInfo, val position: Int, val basePosition: Int)
}

private fun VideoIndexEntity.toSample() = com.frameout.engine.index.SampleInfo(sampleIndex, ptsUs, isKeyframe, nearestKeyframeIndex)
