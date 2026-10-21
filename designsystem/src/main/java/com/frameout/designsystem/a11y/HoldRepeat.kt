package com.frameout.designsystem.a11y

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import android.os.SystemClock
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics

private const val HOLD_DELAY_MS = 350L
private const val REPEAT_MS = 90L

fun Modifier.frameHoldRepeat(onStep: () -> Unit): Modifier = composed {
    val haptics = rememberFrameHaptics()
    val step by rememberUpdatedState(onStep)
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            haptics.perform(FrameHaptic.Tap)
            step()
            if (!heldPast(HOLD_DELAY_MS)) return@awaitEachGesture
            do { step() } while (heldPast(REPEAT_MS))
        }
    }.semantics {
        role = Role.Button
        onClick { step(); true }
    }.touchTarget()
}

private suspend fun AwaitPointerEventScope.heldPast(ms: Long): Boolean = try {
    withTimeout(ms) { waitForUpOrCancellation() }
    false
} catch (_: PointerEventTimeoutCancellationException) {
    true
}

fun Modifier.frameHoldAccelerate(onStep: (Int) -> Unit): Modifier = composed {
    val haptics = rememberFrameHaptics()
    val step by rememberUpdatedState(onStep)
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            haptics.perform(FrameHaptic.Tap)
            step(1)
            if (!heldPast(HOLD_DELAY_MS)) return@awaitEachGesture
            val begin = SystemClock.uptimeMillis()
            var stage = 0
            do {
                val now = ACCEL_STAGES.indexOfLast { SystemClock.uptimeMillis() - begin >= it.afterMs }
                if (now > stage) {
                    stage = now
                    haptics.perform(FrameHaptic.Select)
                }
                step(ACCEL_STAGES[stage].frames)
            } while (heldPast(ACCEL_STAGES[stage].intervalMs))
        }
    }.semantics {
        role = Role.Button
        onClick { step(1); true }
    }.touchTarget()
}

private class AccelStage(val afterMs: Long, val frames: Int, val intervalMs: Long)

private val ACCEL_STAGES = listOf(
    AccelStage(0L, 1, 90L),
    AccelStage(900L, 2, 70L),
    AccelStage(1_800L, 4, 60L),
    AccelStage(3_000L, 8, 55L),
    AccelStage(4_500L, 16, 50L)
)

private const val SCRUB_HOLD_MS = 320L
private const val SCRUB_BOOST_FROM = 3

private class ScrubStage(val afterMs: Long, val frames: Int, val intervalMs: Long)

private val SCRUB_STAGES = listOf(
    ScrubStage(0L, 1, 170L),
    ScrubStage(900L, 1, 95L),
    ScrubStage(2_000L, 2, 80L),
    ScrubStage(3_400L, 4, 70L),
    ScrubStage(5_200L, 8, 65L)
)

fun Modifier.frameHoldScrub(boost: Int = 1, onSpeed: (Int) -> Unit = {}, onStep: (Int) -> Unit): Modifier = composed {
    val haptics = rememberFrameHaptics()
    val step by rememberUpdatedState(onStep)
    val speed by rememberUpdatedState(onSpeed)
    pointerInput(boost) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            haptics.perform(FrameHaptic.Tap)
            step(1)
            if (!heldPast(SCRUB_HOLD_MS)) return@awaitEachGesture
            try {
                scrub(boost, haptics, { step(it) }, { speed(it) })
            } finally {
                speed(1)
            }
        }
    }.semantics {
        role = Role.Button
        onClick { step(1); true }
    }.touchTarget()
}

private suspend fun AwaitPointerEventScope.scrub(boost: Int, haptics: com.frameout.designsystem.haptics.FrameHaptics, step: (Int) -> Unit, speed: (Int) -> Unit) {
    val begin = SystemClock.uptimeMillis()
    var stage = -1
    do {
        val reached = SCRUB_STAGES.indexOfLast { SystemClock.uptimeMillis() - begin >= it.afterMs }
        if (reached > stage) {
            if (stage >= 0) haptics.perform(FrameHaptic.Select)
            stage = reached
            speed(scrubFrames(stage, boost))
        }
        step(scrubFrames(stage, boost))
    } while (heldPast(SCRUB_STAGES[stage].intervalMs))
}

private fun scrubFrames(stage: Int, boost: Int): Int {
    val base = SCRUB_STAGES[stage].frames
    return if (stage >= SCRUB_BOOST_FROM) base * boost.coerceAtLeast(1) else base
}
