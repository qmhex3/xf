package com.frameout.app.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun LibraryEffects(state: LibraryState, vm: LibraryViewModel) {
    val haptics = rememberFrameHaptics()
    LaunchedEffect(state.message) {
        if (state.message != null) {
            kotlinx.coroutines.delay(com.frameout.designsystem.components.toastDurationMs(state.message.orEmpty()))
            vm.clearMessage()
        }
    }
    LaunchedEffect(state.flashSuccess) {
        if (state.flashSuccess) {
            haptics.perform(FrameHaptic.Confirm)
            kotlinx.coroutines.delay(1400)
            vm.clearFlash()
        }
    }
}

@Composable
internal fun BottomScrim(bottomPad: Dp, modifier: Modifier) {
    val bg = FrameTheme.colors.background
    Box(modifier.fillMaxWidth().height(bottomPad + 28.dp).background(Brush.verticalGradient(listOf(Color.Transparent, bg.copy(alpha = 0.90f)))))
}
