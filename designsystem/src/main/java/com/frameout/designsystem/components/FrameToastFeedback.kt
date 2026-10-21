package com.frameout.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.motion.fxShake

@Composable
internal fun Modifier.toastFeedback(kind: ToastKind, message: String): Modifier {
    val haptics = rememberFrameHaptics()
    LaunchedEffect(message, kind) {
        when (kind) {
            ToastKind.Success -> haptics.perform(FrameHaptic.Confirm)
            ToastKind.Error -> haptics.perform(FrameHaptic.Reject)
            ToastKind.Info -> Unit
        }
    }
    return if (kind == ToastKind.Error) fxShake(message) else this
}
