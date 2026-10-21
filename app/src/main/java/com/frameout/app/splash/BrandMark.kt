package com.frameout.app.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameTheme

private const val MARK_MS = 1500

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 112.dp) {
    val accent = FrameTheme.accent
    val paths = remember { SplashPaths() }
    val reduce = LocalSystemA11y.current.reduceMotion
    val haptics = rememberFrameHaptics()
    val clock = remember { Animatable(if (reduce) MARK_MS.toFloat() else 0f) }
    var replay by remember { mutableIntStateOf(0) }
    LaunchedEffect(replay) {
        if (reduce) return@LaunchedEffect
        clock.snapTo(0f)
        clock.animateTo(MARK_MS.toFloat(), tween(MARK_MS, easing = LinearEasing))
    }
    Canvas(
        modifier.size(size).clickable(remember { MutableInteractionSource() }, null) {
            haptics.perform(FrameHaptic.Tap)
            replay += 1
        }
    ) {
        drawBrandTile(clock.value, Offset(this.size.width / 2f, this.size.height / 2f), this.size.minDimension * 0.8f, accent, paths)
    }
}
