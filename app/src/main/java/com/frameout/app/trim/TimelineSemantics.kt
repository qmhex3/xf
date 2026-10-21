package com.frameout.app.trim

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import com.frameout.core.util.FrameA11y
import com.frameout.engine.timeline.TrimRange
import com.frameout.engine.timeline.formatTimestamp

private const val STEP_MS = 1000L
private const val MIN_GAP_MS = 100L

fun Modifier.timelineSemantics(durationMs: Long, positionMs: Long, range: TrimRange, onSeek: (Long) -> Unit, onRangeChange: (TrimRange) -> Unit): Modifier {
    val dur = durationMs.coerceAtLeast(1)
    fun moveStart(delta: Long): Boolean {
        val start = (range.startMs + delta).coerceIn(0L, range.endMs - MIN_GAP_MS)
        onRangeChange(range.copy(startMs = start, useFull = false))
        onSeek(start)
        return true
    }
    fun moveEnd(delta: Long): Boolean {
        val end = (range.endMs + delta).coerceIn(range.startMs + MIN_GAP_MS, dur)
        onRangeChange(range.copy(endMs = end, useFull = false))
        onSeek(end)
        return true
    }
    return semantics {
        contentDescription = FrameA11y.timeline()
        stateDescription = FrameA11y.timelineState(formatTimestamp(positionMs), formatTimestamp(range.startMs), formatTimestamp(range.endMs))
        progressBarRangeInfo = ProgressBarRangeInfo(positionMs.toFloat().coerceIn(0f, dur.toFloat()), 0f..dur.toFloat())
        setProgress { value -> onSeek(value.toLong().coerceIn(0L, dur)); true }
        customActions = listOf(
            CustomAccessibilityAction(FrameA11y.startEarlier()) { moveStart(-STEP_MS) },
            CustomAccessibilityAction(FrameA11y.startLater()) { moveStart(STEP_MS) },
            CustomAccessibilityAction(FrameA11y.endEarlier()) { moveEnd(-STEP_MS) },
            CustomAccessibilityAction(FrameA11y.endLater()) { moveEnd(STEP_MS) }
        )
    }
}
