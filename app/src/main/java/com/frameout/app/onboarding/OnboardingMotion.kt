package com.frameout.app.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.frameout.designsystem.a11y.LocalSystemA11y

internal object OnbColors {
    val Indigo = Color(0xFF6366F1)
    val Violet = Color(0xFF8B5CF6)
    val Cyan = Color(0xFF06B6D4)
    val CyanSoft = Color(0xFF4CD7F6)
    val Ink = Color(0xFF0B0C1F)
    val Night = Color(0xFF15173D)
    val Ring = Color(0xFF8E91FF)
}

@Composable
internal fun rememberLoop(
    durationMs: Int,
    still: Float,
    reverse: Boolean,
    label: String,
    ease: Boolean = false
): State<Float> {
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    return if (reduceMotion) {
        remember(still) { mutableFloatStateOf(still) }
    } else {
        rememberInfiniteTransition(label = label).animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = if (ease) FastOutSlowInEasing else LinearEasing),
                repeatMode = if (reverse) RepeatMode.Reverse else RepeatMode.Restart
            ),
            label = label
        )
    }
}
