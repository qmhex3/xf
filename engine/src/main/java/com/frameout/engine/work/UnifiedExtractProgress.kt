package com.frameout.engine.work

import com.frameout.engine.extract.ExtractProgress
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object UnifiedExtractProgress {
    const val INDEX_SHARE = 0.2f

    fun raw(status: String, done: Int, total: Int, indexingPhase: Boolean): Float {
        if (status == "Completed") return 1f
        if (indexingPhase) {
            val denominator = maxOf(total, done)
            return if (denominator > 0) INDEX_SHARE * (done.toFloat() / denominator).coerceIn(0f, 1f) else 0f
        }
        return if (total > 0) INDEX_SHARE + (1f - INDEX_SHARE) * (done.toFloat() / total).coerceIn(0f, 1f) else INDEX_SHARE
    }

    fun stored(status: String, extracted: Int, total: Int, live: Float?): Float? {
        if (status == "Completed") return 1f
        val active = status == "Indexing" || status == "Extracting" || status == "Paused"
        if (!active) return null
        val fromDb = if (extracted > 0 && total > 0) raw("Extracting", extracted, total, false) else null
        val value = when {
            live != null && fromDb != null -> maxOf(live, fromDb)
            live != null -> live
            else -> fromDb
        }
        return value?.coerceIn(0f, 1f)
    }
}

object LiveExtractProgress {
    private val values = ConcurrentHashMap<Long, Float>()
    private val _all = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val all: StateFlow<Map<Long, Float>> = _all.asStateFlow()

    fun publish(projectId: Long, fraction: Float) {
        val previous = values[projectId]
        val next = if (previous == null) fraction else maxOf(previous, fraction)
        values[projectId] = next
        if (previous != next) _all.value = HashMap(values)
    }

    fun clear(projectId: Long) {
        if (values.remove(projectId) != null) _all.value = HashMap(values)
    }

    fun observe(projectId: Long) = all.map { it[projectId] }.distinctUntilChanged()
}

internal class UnifiedProgressMeter {
    private var indexing: Boolean? = null
    private var peak = 0f

    fun update(p: ExtractProgress): Float {
        when (p.status) {
            ExtractProgress.Status.Indexing -> indexing = true
            ExtractProgress.Status.Extracting -> indexing = false
            else -> Unit
        }
        val raw = UnifiedExtractProgress.raw(p.status.name, p.done, p.total, indexing ?: true)
        peak = if (p.status == ExtractProgress.Status.Completed) 1f else maxOf(peak, raw)
        return peak
    }

    val isIndexing: Boolean get() = indexing == true
}
