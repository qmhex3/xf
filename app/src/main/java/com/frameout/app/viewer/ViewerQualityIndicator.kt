package com.frameout.app.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameIslandToast
import com.frameout.designsystem.components.IslandToastState
import com.frameout.designsystem.components.ToastKind
import kotlinx.coroutines.delay

enum class QualityPhase { None, Loading, Improving, Failed }

private const val SHOW_DELAY_MS = 180L

internal fun ViewerState.qualityPhase(): QualityPhase = when {
    current == null -> if (loading) QualityPhase.Loading else QualityPhase.None
    bitmap != null -> QualityPhase.None
    placeholder != null -> QualityPhase.Improving
    loading -> QualityPhase.Loading
    else -> QualityPhase.Failed
}

private fun QualityPhase.toast(): IslandToastState? = when (this) {
    QualityPhase.None -> null
    QualityPhase.Improving -> IslandToastState(FrameStrings.viewerImproving(), kind = ToastKind.Info, busy = true)
    QualityPhase.Failed -> IslandToastState(FrameStrings.viewerLoadFailed(), kind = ToastKind.Error)
    QualityPhase.Loading -> IslandToastState(FrameStrings.viewerLoadingFrame(), kind = ToastKind.Info, busy = true)
}

@Composable
fun ViewerQualityIndicator(phase: QualityPhase, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(QualityPhase.None) }
    LaunchedEffect(phase) {
        if (phase != QualityPhase.None && shown == QualityPhase.None) delay(SHOW_DELAY_MS)
        shown = phase
    }
    val state = remember(shown) { shown.toast() }
    FrameIslandToast(state, modifier)
}
