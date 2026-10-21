package com.frameout.app.billing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure

internal class BoltColors(val first: Color, val second: Color, val third: Color) {
    fun at(i: Int): Color = when (i % 4) {
        0 -> first
        1 -> second
        2 -> third
        else -> Color.White
    }
}

internal object BoltGeo {
    const val HEIGHT = 140f
    private const val HALF_W = 50f
    private const val HALF_H = 70f
    private val points = listOf(62f to 0f, 14f to 78f, 46f to 78f, 34f to 140f, 90f to 52f, 56f to 52f, 76f to 0f)

    fun path(scale: Float, center: Offset): Path = Path().apply {
        points.forEachIndexed { i, (x, y) ->
            val px = center.x + (x - HALF_W) * scale
            val py = center.y + (y - HALF_H) * scale
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
}

internal fun seg(t: Float, from: Float, to: Float): Float {
    val x = ((t - from) / (to - from)).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

internal fun easeOut(p: Float): Float = 1f - (1f - p) * (1f - p) * (1f - p)

internal fun Path.trimmed(fraction: Float): Path {
    val measure = PathMeasure()
    measure.setPath(this, false)
    val out = Path()
    measure.getSegment(0f, measure.length * fraction.coerceIn(0f, 1f), out, true)
    return out
}

internal fun unitHash(seed: Float): Float {
    val v = kotlin.math.sin(seed * 12.9898f) * 43758.547f
    return v - kotlin.math.floor(v)
}
