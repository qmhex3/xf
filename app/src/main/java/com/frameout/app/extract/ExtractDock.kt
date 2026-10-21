package com.frameout.app.extract

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.dock.ChargeState
import com.frameout.designsystem.dock.FrameChargeFab
import com.frameout.designsystem.dock.DockItem
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.engine.work.ExtractScheduler

private class ExtractFabLook(val charge: ChargeState, val icon: Int, val label: String)

private fun succeeded(state: ExtractCapsuleState) = state.status == "Completed" || state.status == "SUCCEEDED"

private fun failed(state: ExtractCapsuleState) = state.status == "Failed" || state.status == "FAILED"

private fun fabLook(state: ExtractCapsuleState): ExtractFabLook = when {
    state.finished && succeeded(state) -> ExtractFabLook(ChargeState.Success, FrameIcons.Check, FrameStrings.completed())
    state.finished && failed(state) -> ExtractFabLook(ChargeState.Failed, FrameIconsExtra.Resume, FrameStrings.retry())
    state.finished -> ExtractFabLook(ChargeState.Failed, FrameIcons.Close, FrameStrings.close())
    state.paused -> ExtractFabLook(ChargeState.Paused, FrameIconsExtra.Resume, FrameStrings.resume())
    else -> ExtractFabLook(ChargeState.Running, FrameIcons.Pause, FrameStrings.pause())
}

private const val TAP_GUARD_MS = 800L

fun extractFabLabel(state: ExtractCapsuleState): String {
    val look = fabLook(state)
    val working = look.charge == ChargeState.Running || look.charge == ChargeState.Paused
    if (!working || state.shownTotal <= 0) return look.label
    val progress = "\u2066" + state.counter.ifEmpty { "${state.percent}%" } + "\u2069"
    val running = look.charge == ChargeState.Running && !state.estimated
    return when {
        running && state.remaining > 0L -> "$progress · ${formatExtractTime(state.remaining)}"
        running -> progress
        else -> "${look.label} $progress"
    }
}

fun extractCancelItem(state: ExtractCapsuleState, onCancel: () -> Unit): DockItem? =
    if (state.finished) null
    else DockItem("cancelExtract", FrameStrings.cancelExtract(), FrameIcons.Close, destructive = true, onClick = onCancel)

@Composable
private fun ExtractOutcomeHaptic(state: ExtractCapsuleState) {
    val haptics = rememberFrameHaptics()
    val watched = remember { booleanArrayOf(false) }
    LaunchedEffect(state.finished, state.status) {
        if (!state.finished) {
            watched[0] = state.status.isNotEmpty()
            return@LaunchedEffect
        }
        if (!watched[0]) return@LaunchedEffect
        watched[0] = false
        if (succeeded(state)) haptics.perform(FrameHaptic.Confirm) else if (failed(state)) haptics.perform(FrameHaptic.Reject)
    }
}

@Composable
fun ExtractFab(state: ExtractCapsuleState, projectId: Long, onDone: () -> Unit, onClose: () -> Unit = onDone) {
    val context = LocalContext.current
    ExtractOutcomeHaptic(state)
    val look = fabLook(state)
    var leaving by remember { mutableStateOf(false) }
    var lastTapMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(leaving) {
        if (leaving) {
            kotlinx.coroutines.delay(1500)
            leaving = false
        }
    }
    FrameChargeFab(
        state = look.charge,
        icon = look.icon,
        contentDescription = look.label,
        onClick = click@{
            if (leaving) return@click
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastTapMs < TAP_GUARD_MS) return@click
            lastTapMs = now
            if (state.finished && !failed(state)) leaving = true
            when {
                state.finished && failed(state) -> ExtractScheduler.resume(context, projectId)
                state.finished && succeeded(state) -> onDone()
                state.finished -> onClose()
                state.paused -> ExtractScheduler.resume(context, projectId)
                else -> ExtractScheduler.pause(context, projectId)
            }
        },
        progress = state.fraction.takeIf { state.shownTotal > 0 },
        percentText = "${state.percent}%"
    )
}
