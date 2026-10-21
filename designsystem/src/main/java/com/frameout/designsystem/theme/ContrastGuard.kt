package com.frameout.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

object ContrastGuard {
    const val TEXT = 4.5f
    const val BOUNDARY = 3f

    fun ratio(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    fun satisfies(color: Color, backgrounds: List<Color>, minimum: Float): Boolean = backgrounds.all { ratio(color, it) >= minimum }

    fun ensure(color: Color, backgrounds: List<Color>, minimum: Float, towardLight: Boolean): Color {
        val opaque = color.copy(alpha = 1f)
        if (satisfies(opaque, backgrounds, minimum)) return opaque
        val end = if (towardLight) Color.White else Color.Black
        var step = 1
        while (step <= STEPS) {
            val candidate = lerp(opaque, end, step / STEPS.toFloat())
            if (satisfies(candidate, backgrounds, minimum)) return candidate
            step++
        }
        return end
    }

    fun ensureAgainst(color: Color, background: Color, minimum: Float): Color {
        val towardLight = background.luminance() < color.luminance() || background.luminance() < 0.18f
        return ensure(color, listOf(background), minimum, towardLight)
    }

    fun readableOn(background: Color, preferred: List<Color>): Color {
        preferred.firstOrNull { ratio(it, background) >= TEXT }?.let { return it }
        return listOf(Color.White, Color.Black).maxByOrNull { ratio(it, background) } ?: Color.Black
    }

    private const val STEPS = 50
}
