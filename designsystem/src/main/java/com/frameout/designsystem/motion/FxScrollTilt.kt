package com.frameout.designsystem.motion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.frameout.designsystem.a11y.LocalSystemA11y
import kotlin.math.abs

@Composable
fun Modifier.fxScrollTilt(maxTilt: Float = 30f, position: () -> Float): Modifier {
    if (LocalSystemA11y.current.reduceMotion) return this
    return graphicsLayer {
        val d = position().coerceIn(-1.2f, 1.2f)
        val a = abs(d).coerceAtMost(1f)
        val s = 1f - a * 0.07f
        cameraDistance = 14f * density
        rotationX = -d * maxTilt
        scaleX = s
        scaleY = s
        alpha = 1f - a * 0.45f
    }
}
