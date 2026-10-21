package com.frameout.app.billing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.delay

private const val INTRO_MS = 3200
private const val PULSE_MS = 1400

@Composable
fun ProUnlockedDialog(onDone: () -> Unit) {
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        RetintedTheme(null) { ProUnlockedContent(onDone) }
    }
}

@Composable
private fun ProUnlockedContent(onDone: () -> Unit) {
    val colors = FrameTheme.colors
    val reduce = LocalSystemA11y.current.reduceMotion
    val intro = rememberIntro(reduce)
    val pulse = rememberPulse(reduce)
    val palette = BoltColors(colors.accentC, colors.accentA, colors.accentB)
    UnlockHaptics(reduce)
    Box(Modifier.fillMaxSize().drawBehind { drawScrim(colors.accentA) }) {
        Canvas(Modifier.fillMaxSize()) { drawBoltScene(intro.value, pulse.value, palette) }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UnlockCopy(intro)
            DoneChips(intro, palette)
            DoneCta(Modifier.padding(top = 22.dp).revealAt(intro, 0.80f, 0.95f), onDone)
        }
    }
}

@Composable
private fun UnlockCopy(intro: State<Float>) {
    val colors = FrameTheme.colors
    val description = ProStrings.doneTitle() + ". " + ProStrings.doneBody()
    Column(Modifier.semantics { contentDescription = description }.padding(bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FrameText(ProStrings.doneTitle(), FrameTheme.typography.display, colors.onSurface, Modifier.revealAt(intro, 0.42f, 0.62f), TextAlign.Center)
        FrameText(ProStrings.doneBody(), FrameTheme.typography.body, colors.onSurfaceVariant, Modifier.padding(top = 6.dp).revealAt(intro, 0.50f, 0.70f), TextAlign.Center)
    }
}

@Composable
private fun UnlockHaptics(reduce: Boolean) {
    val haptics = rememberFrameHaptics()
    LaunchedEffect(Unit) {
        haptics.perform(FrameHaptic.Confirm)
        if (reduce) return@LaunchedEffect
        delay(280)
        haptics.perform(FrameHaptic.Tick)
        delay(220)
        haptics.perform(FrameHaptic.Tick)
    }
}

private fun DrawScope.drawScrim(accent: Color) {
    val base = Color(0xF2050608)
    val center = Offset(size.width / 2f, size.height * 0.27f)
    drawRect(Brush.radialGradient(listOf(lerp(base, accent, 0.30f), base), center, size.maxDimension * 0.9f))
}

@Composable
private fun rememberIntro(reduce: Boolean): State<Float> {
    val t = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(reduce) { if (!reduce) t.animateTo(1f, tween(INTRO_MS, easing = LinearEasing)) }
    return t.asState()
}

@Composable
private fun rememberPulse(reduce: Boolean): State<Float> {
    if (reduce) return remember { mutableFloatStateOf(0.5f) }
    val transition = rememberInfiniteTransition(label = "boltPulse")
    return transition.animateFloat(0f, 1f, infiniteRepeatable(tween(PULSE_MS), RepeatMode.Reverse), label = "boltPulseValue")
}
