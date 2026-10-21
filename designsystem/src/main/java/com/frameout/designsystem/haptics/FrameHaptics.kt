package com.frameout.designsystem.haptics

import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

enum class FrameHaptic {
    Tap,
    Select,
    Tick,
    LongPress,
    DragStart,
    DragEnd,
    Confirm,
    Reject
}

private const val TICK_GAP_MS = 45L

private fun FrameHaptic.constant(): Int = when (this) {
    FrameHaptic.Tap -> HapticFeedbackConstants.VIRTUAL_KEY
    FrameHaptic.Select -> if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
    FrameHaptic.Tick -> if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK else HapticFeedbackConstants.CLOCK_TICK
    FrameHaptic.LongPress -> HapticFeedbackConstants.LONG_PRESS
    FrameHaptic.DragStart -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.GESTURE_START else HapticFeedbackConstants.VIRTUAL_KEY
    FrameHaptic.DragEnd -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.GESTURE_END else HapticFeedbackConstants.CLOCK_TICK
    FrameHaptic.Confirm -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
    FrameHaptic.Reject -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.VIRTUAL_KEY
}

@Stable
class FrameHaptics(private val view: View) {
    private var lastTick = 0L

    fun perform(kind: FrameHaptic) {
        if (kind == FrameHaptic.Tick) {
            val now = SystemClock.uptimeMillis()
            if (now - lastTick < TICK_GAP_MS) return
            lastTick = now
        }
        view.performHapticFeedback(kind.constant())
    }
}

@Composable
fun rememberFrameHaptics(): FrameHaptics {
    val view = LocalView.current
    return remember(view) { FrameHaptics(view) }
}

@Composable
fun HapticOnChange(key: Any?, enabled: Boolean = true, kind: FrameHaptic = FrameHaptic.Tick) {
    val haptics = rememberFrameHaptics()
    val skip = remember { booleanArrayOf(true) }
    LaunchedEffect(key) {
        if (skip[0]) {
            skip[0] = false
            return@LaunchedEffect
        }
        if (enabled) haptics.perform(kind)
    }
}
