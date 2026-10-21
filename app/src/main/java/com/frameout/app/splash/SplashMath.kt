package com.frameout.app.splash

import kotlin.math.pow

internal fun seg(t: Float, from: Float, to: Float): Float = ((t - from) / (to - from)).coerceIn(0f, 1f)

internal fun outCubic(x: Float): Float = 1f - (1f - x).pow(3)

internal fun inOut(x: Float): Float = x * x * (3f - 2f * x)

internal fun outBack(x: Float): Float {
    val k = 1.70158f
    val c = x - 1f
    return 1f + (k + 1f) * c * c * c + k * c * c
}
