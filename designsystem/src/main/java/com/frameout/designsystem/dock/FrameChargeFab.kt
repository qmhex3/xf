package com.frameout.designsystem.dock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.frameout.designsystem.theme.FrameColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.haptics.FrameHaptic
import com.frameout.designsystem.haptics.rememberFrameHaptics
import com.frameout.designsystem.motion.LaunchOrigin
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.motion.trackLaunchOrigin
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

enum class ChargeState { Idle, Running, Paused, Success, Failed }


private const val CHARGE_DEPTH = 0.12f
private const val WAVE_UNDER_TEXT = 0.55f

private const val INDETERMINATE_LEVEL = 0.22f

private const val SQUIRCLE = 0.5f

@Composable
fun FrameChargeFab(
    state: ChargeState,
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    percentText: String? = null
) {
    val colors = FrameTheme.colors
    val haptics = rememberFrameHaptics()
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val running = state == ChargeState.Running
    val working = running || state == ChargeState.Paused
    val success = state == ChargeState.Success
    val failed = state == ChargeState.Failed
    val press by animateFloatAsState(if (pressed) 0.93f else 1f, frameSpring(), label = "chargePress")
    val pop by animateFloatAsState(if (success) 1.08f else 1f, frameSpring(), label = "chargePop")
    val workT by animateFloatAsState(if (working) 1f else 0f, frameSpring(), label = "chargeWork")
    val successT by animateFloatAsState(if (success) 1f else 0f, frameSpring(), label = "chargeSuccess")
    val failedT by animateFloatAsState(if (failed) 1f else 0f, frameSpring(), label = "chargeFailed")
    val energy by animateFloatAsState(if (running) 1f else 0f, tween(450), label = "chargeEnergy")
    val corner by animateFloatAsState(if (state == ChargeState.Idle) SQUIRCLE else 0.5f, frameSpring(), label = "chargeCorner")
    val level by animateFloatAsState(
        targetValue = if (progress != null) progress.coerceIn(0f, 1f) else INDETERMINATE_LEVEL,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "chargeLevel"
    )
    val glowColor by animateColorAsState(
        when {
            success -> colors.success
            failed -> colors.error
            else -> colors.primary
        },
        frameSpring(),
        label = "chargeGlowColor"
    )
    val restingGlow = when {
        running -> 0.24f
        working -> 0.12f
        success || failed -> 0.28f
        else -> FabRestGlow
    }
    val pulse: State<Float>? = if (running && !reduceMotion) {
        rememberInfiniteTransition(label = "chargeGlow").animateFloat(
            initialValue = 0.16f,
            targetValue = 0.34f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "chargeGlowValue"
        )
    } else {
        null
    }
    val content by animateColorAsState(chargeContent(colors, working, success, failed, level), frameSpring(), label = "chargeContent")
    val shape = RoundedCornerShape(percent = (corner * 100f).toInt().coerceIn(0, 50))
    val orbit = rememberOrbit(running, progress, workT)
    Box(
        modifier
            .size(FrameFabSize)
            .trackLaunchOrigin()
            .scale(press * pop)
            .frameGlow(glowColor, { pulse?.value ?: restingGlow }, FabGlowSpread, { corner })
            .then(if (workT > 0.01f) Modifier.drawBehind { drawOrbit(orbit) } else Modifier),
        Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(FrameGradients.Fab)
                .background(colors.surface.copy(alpha = workT))
                .background(colors.success.copy(alpha = successT))
                .background(colors.error.copy(alpha = failedT))
                .background(FabSheen)
                .border(1.dp, FabRim, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        haptics.perform(FrameHaptic.Tap)
                        LaunchOrigin.mark(colors.primary)
                        onClick()
                    }
                )
                .semantics { percentText?.let { stateDescription = it } },
            contentAlignment = Alignment.Center
        ) {
            if (working) ChargeLayer(level = { level }, energy = { energy }, running = running)
            FabContent(icon, contentDescription, percentText.takeIf { working && progress != null }, reduceMotion, content)
        }
    }
}

@Composable
private fun FabContent(icon: Int, contentDescription: String, percent: String?, reduceMotion: Boolean, content: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                if (reduceMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    (fadeIn(tween(220, delayMillis = 60)) + scaleIn(tween(260), initialScale = 0.55f)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(tween(160), targetScale = 0.55f))
                }
            },
            label = "chargeIcon"
        ) { current ->
            Image(
                painter = painterResource(current),
                contentDescription = contentDescription,
                modifier = Modifier.size(if (percent != null) 22.dp else 26.dp),
                colorFilter = ColorFilter.tint(content)
            )
        }
        if (percent != null) {
            FrameText(
                percent,
                style = FrameTheme.typography.monoSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    shadow = Shadow(Color.Black.copy(alpha = if (content.luminance() > 0.5f) 0.4f else 0f), blurRadius = 3f)
                ),
                color = content
            )
        }
    }
}

private fun readableOn(background: Color): Color = if (background.luminance() > 0.5f) Color(0xFF0E1110) else Color.White

private fun chargeContent(scheme: FrameColorScheme, working: Boolean, success: Boolean, failed: Boolean, level: Float): Color = when {
    success -> readableOn(scheme.success)
    failed -> readableOn(scheme.error)
    working && level > WAVE_UNDER_TEXT -> readableOn(lerp(scheme.accentA, Color.Black, CHARGE_DEPTH))
    working -> scheme.onSurface
    else -> Color.White
}

@Composable
private fun phaseState(running: Boolean, periodMs: Int): State<Float> {
    val animate = running && !LocalSystemA11y.current.reduceMotion
    return if (animate) {
        rememberInfiniteTransition(label = "chargePhase").animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing)),
            label = "chargePhaseValue"
        )
    } else {
        remember { mutableStateOf(0f) }
    }
}

@Composable
private fun ChargeLayer(level: () -> Float, energy: () -> Float, running: Boolean) {
    val phase = phaseState(running, 2600)
    val scheme = FrameTheme.colors
    val stops = remember(scheme.accentA, scheme.accentB, scheme.accentC) {
        listOf(scheme.accentC, scheme.accentA, scheme.accentB).map { lerp(it, Color.Black, CHARGE_DEPTH) }
    }
    Canvas(Modifier.fillMaxSize()) {
        val lvl = level()
        val e = energy()
        val amp = 2.6.dp.toPx() * e * min(1f, min(lvl, 1f - lvl) * 8f)
        drawWave(lvl, -phase.value * 1.3f + 1.7f, amp * 1.35f, 1.0f, alpha = 0.35f * (0.6f + 0.4f * e), crest = false, stops = stops)
        drawWave(lvl, phase.value, amp, 1.25f, alpha = 0.55f + 0.45f * e, crest = true, stops = stops)
    }
}

private fun DrawScope.drawWave(
    level: Float,
    phase: Float,
    amp: Float,
    freq: Float,
    alpha: Float,
    crest: Boolean,
    stops: List<Color>
) {
    if (level <= 0.001f) return
    val w = size.width
    val h = size.height
    val y = h * (1f - level)
    val steps = 36
    val top = Path()
    for (i in 0..steps) {
        val x = w * i / steps
        val yy = y + amp * sin(x / w * freq * 2f * PI.toFloat() + phase)
        if (i == 0) top.moveTo(x, yy) else top.lineTo(x, yy)
    }
    val body = Path().apply {
        addPath(top)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    val brush = Brush.verticalGradient(
        colors = stops,
        startY = (y - amp).coerceAtLeast(0f),
        endY = h
    )
    drawPath(body, brush, alpha = alpha)
    if (crest) drawPath(top, Color.White.copy(alpha = 0.5f * alpha), style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round))
}
