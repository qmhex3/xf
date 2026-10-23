package com.frameout.app.batch

import android.graphics.Bitmap
import com.frameout.engine.timeline.TrimRange

data class PrepItem(
    val uri: String,
    val name: String,
    val durationMs: Long = 0L,
    val thumb: Bitmap? = null,
    val range: TrimRange? = null,
    val loaded: Boolean = false,
    val probe: com.frameout.app.trim.SourceProbe? = null
) {
    val edited: Boolean get() = range?.useFull == false
    val spanMs: Long get() = if (edited) range!!.spanMs() else durationMs
}

data class BatchItem(
    val projectId: Long,
    val name: String,
    val uri: String,
    val status: String = "Queued",
    val extracted: Int = 0,
    val total: Int = 0,
    val live: Float? = null
) {
    val finished: Boolean get() = status == "Completed" || status == "Failed" || status == "Cancelled"

    private val unified: Float? get() = com.frameout.engine.work.UnifiedExtractProgress.stored(status, extracted, total, live)

    val hasProgress: Boolean get() = unified != null

    val fraction: Float get() = unified ?: 0f
}

data class BatchState(
    val batchId: String = "",
    val items: List<BatchItem> = emptyList(),
    val done: Boolean = false
) {
    val completedCount: Int get() = items.count { it.status == "Completed" }
    val allSucceeded: Boolean get() = items.isNotEmpty() && items.all { it.status == "Completed" }
    val progress: Float get() = if (items.isEmpty()) 0f else items.map { it.fraction }.average().toFloat()
    val active: BatchItem? get() = items.firstOrNull {
        it.status == "Extracting" || it.status == "Indexing" || it.status == "Paused"
    }
}
