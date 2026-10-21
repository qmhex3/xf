package com.frameout.app.frames

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frameout.app.trim.CaptureStatus
import com.frameout.app.trim.CaptureStatusState
import com.frameout.app.trim.SavedCapture
import com.frameout.core.util.FrameStrings

data class ExportOutcome(val message: String, val saved: SavedCapture? = null)

@Composable
internal fun ExportResult(saved: SavedCapture?, onDismiss: () -> Unit, modifier: Modifier, note: String? = null) {
    CaptureStatus(saved?.let { CaptureStatusState(note ?: FrameStrings.savedOk(), it, error = note != null) }, modifier, onDismiss)
}
