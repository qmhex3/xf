package com.frameout.app.extract

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameRollingText
import com.frameout.designsystem.motion.fxSheen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import com.frameout.designsystem.components.FrameConfirmDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.work.ExtractScheduler

@Composable
fun ExtractCapsule(state: ExtractCapsuleState, projectId: Long, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    GlassSurface(
        shape = if (expanded) FrameShapes.lg else FrameShapes.pill,
        modifier = modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth().animateContentSize(frameSpring())
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            CapsuleSummary(state) { expanded = !expanded }
            CapsuleActions(state, projectId, onClose)
            if (expanded) CapsuleDetails(state)
        }
    }
}

@Composable
private fun CapsuleSummary(state: ExtractCapsuleState, onToggle: () -> Unit) {
    val colors = FrameTheme.colors
    Column(Modifier.fillMaxWidth().frameClickable(onClick = onToggle)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExtractFlow(!state.paused && !state.finished, state.finished)
            FrameText(extractStatusLabel(state.status, state.message), style = FrameTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            FrameRollingText("${state.percent}%", FrameTheme.typography.monoSmall, colors.onSurface)
            FrameRollingText(state.counter, FrameTheme.typography.monoSmall, colors.onSurfaceVariant)
        }
        CapsuleTrack(state.fraction, state.percent, !state.paused && !state.finished)
    }
}

@Composable
private fun CapsuleTrack(fraction: Float, percent: Int, running: Boolean) {
    val colors = FrameTheme.colors
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), if (LocalSystemA11y.current.reduceMotion) snap() else tween(350, easing = LinearOutSlowInEasing), label = "capsuleFill")
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp)
            .height(4.dp)
            .background(colors.container, FrameShapes.pill)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction.coerceIn(0f, 1f), 0f..1f, 0)
                contentDescription = "$percent%"
            }
    ) {
        Box(Modifier.fillMaxWidth(shown).height(4.dp).clip(FrameShapes.pill).background(colors.primary).fxSheen(running))
    }
}

@Composable
private fun CapsuleDetails(state: ExtractCapsuleState) {
    val colors = FrameTheme.colors
    val remaining = if (state.remaining > 0L || state.finished) formatExtractTime(state.remaining) else "–"
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        FrameText(FrameStrings.elapsed(formatExtractTime(state.elapsed)), style = FrameTheme.typography.label, color = colors.onSurfaceVariant)
        FrameText(FrameStrings.remaining(remaining), style = FrameTheme.typography.label, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun CapsuleActions(state: ExtractCapsuleState, projectId: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    val colors = FrameTheme.colors
    var confirmCancel by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        if (state.finished) {
            Spacer(Modifier.weight(1f))
            FrameText(FrameStrings.close(), color = colors.primary, modifier = Modifier.frameClickable(onClick = onClose).padding(horizontal = 12.dp, vertical = 12.dp))
            return@Row
        }
        FrameText(
            FrameStrings.cancelExtract(),
            color = colors.error,
            modifier = Modifier.frameClickable { confirmCancel = true }.padding(horizontal = 12.dp, vertical = 12.dp)
        )
        val toggle = { if (state.paused) ExtractScheduler.resume(context, projectId) else ExtractScheduler.pause(context, projectId) }
        FrameText(
            if (state.paused) FrameStrings.resume() else FrameStrings.pause(),
            color = colors.primary,
            modifier = Modifier.frameClickable { toggle() }.padding(horizontal = 12.dp, vertical = 12.dp)
        )
    }
    if (confirmCancel) {
        FrameConfirmDialog(
            title = FrameStrings.confirmCancelExtract(),
            body = FrameStrings.confirmCancelExtractBody(),
            confirmLabel = FrameStrings.cancelExtract(),
            dismissLabel = FrameStrings.keepExtracting(),
            onConfirm = { confirmCancel = false; ExtractScheduler.cancel(context, projectId) },
            onDismiss = { confirmCancel = false },
            destructive = true,
            icon = com.frameout.designsystem.icons.FrameIcons.Close
        )
    }
}
