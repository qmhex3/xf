package com.frameout.app.compare

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.frameout.designsystem.a11y.LocalSystemA11y
import kotlinx.coroutines.delay

@Composable
internal fun CompareIntroNudge(split: MutableFloatState, ready: Boolean, isDragging: () -> Boolean) {
    val reduce = LocalSystemA11y.current.reduceMotion
    var played by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ready) {
        if (!ready || played || reduce) return@LaunchedEffect
        played = true
        delay(500)
        val anim = Animatable(split.floatValue)
        for (target in floatArrayOf(0.68f, 0.32f, 0.5f)) {
            if (isDragging()) return@LaunchedEffect
            anim.animateTo(target, tween(480, easing = FastOutSlowInEasing)) {
                if (!isDragging()) split.floatValue = value
            }
        }
    }
}
