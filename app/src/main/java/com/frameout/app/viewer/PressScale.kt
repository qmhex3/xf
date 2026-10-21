package com.frameout.app.viewer

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.frameout.designsystem.a11y.LocalSystemA11y

internal fun Modifier.pressScale(pressedScale: Float = 0.94f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val reduce = LocalSystemA11y.current.reduceMotion
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = if (reduce) snap() else spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "pressScale"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            pressed = true
            waitForUpOrCancellation()
            pressed = false
        }
    }
}
