package com.frameout.app.trim

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.ClipStrings
import com.frameout.core.util.ExtractFabStrings
import com.frameout.core.util.FrameStrings
import com.frameout.engine.timeline.TrimRange
import kotlinx.coroutines.Job

internal class TrimUiState(autoSave: Boolean, output: TrimOutput = TrimOutput.Frames) {
    var range by mutableStateOf(TrimRange())
    var output by mutableStateOf(output)
    var sheet by mutableStateOf(TrimSheet.None)
    var status by mutableStateOf<CaptureStatusState?>(null)
    var busy by mutableStateOf(false)
    var autoSave by mutableStateOf(autoSave)
    var saveJob: Job? = null
    private var leaveArmed = false
    private var lastPercent = -1

    fun showFull(durationMs: Long) {
        range = TrimLimits.fit(TrimRange.full(durationMs), output.maxMs)
    }

    fun selectOutput(next: TrimOutput, positionMs: Long = 0L) {
        output = next
        range = TrimLimits.fitAt(range, next.maxMs, positionMs)
    }

    fun change(next: TrimRange) {
        range = TrimLimits.enforce(range, next, output.maxMs)
    }

    fun toggleAutoSave(prefs: AppPrefs) {
        autoSave = !autoSave
        prefs.setAutoSave(autoSave)
    }

    fun holdBack(): Boolean {
        if (!busy) return false
        if (leaveArmed) {
            saveJob?.cancel()
            return false
        }
        leaveArmed = true
        showProgress()
        return true
    }

    fun progress(percent: Int) {
        if (!busy || percent <= lastPercent) return
        lastPercent = percent
        showProgress()
    }

    private fun showProgress() {
        val head = if (leaveArmed) ClipStrings.saveWait() else FrameStrings.saving()
        status = CaptureStatusState(if (lastPercent < 0) head else "$head ${ExtractFabStrings.percent(lastPercent)}", busy = true)
    }

    fun saveEnded() {
        busy = false
        lastPercent = -1
        leaveArmed = false
        saveJob = null
    }
}
