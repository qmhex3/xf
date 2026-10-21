package com.frameout.designsystem.dock

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

internal val NotchRadius = FrameFabSize / 2 + 6.dp

internal val FabRaise = 14.dp

internal val NotchDepth = NotchRadius - FabRaise

private val NotchFillet = 14.dp

internal data class NotchedPillShape(val centerX: Float, val depth: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val r = h / 2f
        val d = with(density) { depth.dp.toPx() }
        if (d < 0.5f) {
            return Outline.Rounded(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, androidx.compose.ui.geometry.CornerRadius(r)))
        }
        val big = with(density) { NotchRadius.toPx() }
        val f = with(density) { NotchFillet.toPx() }
        val cx = with(density) { centerX.dp.toPx() }
        val cy = d - big
        val dy = f - cy
        val dx = sqrt(((big + f) * (big + f) - dy * dy).coerceAtLeast(0f))
        val deg = (180.0 / PI).toFloat()
        fun norm(a: Float) = ((a % 360f) + 360f) % 360f

        val aFL = norm(atan2(cy - f, dx) * deg)
        val aCL = norm(atan2(f - cy, -dx) * deg)
        val aCR = norm(atan2(f - cy, dx) * deg)
        val aFR = norm(atan2(cy - f, -dx) * deg)

        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(cx - dx, 0f)
            arcTo(Rect(cx - dx - f, 0f, cx - dx + f, 2 * f), 270f, norm(aFL - 270f), false)
            arcTo(Rect(cx - big, cy - big, cx + big, cy + big), aCL, -(aCL - aCR), false)
            arcTo(Rect(cx + dx - f, 0f, cx + dx + f, 2 * f), aFR, norm(270f - aFR), false)
            lineTo(w - r, 0f)
            arcTo(Rect(w - 2 * r, 0f, w, h), 270f, 180f, false)
            lineTo(r, h)
            arcTo(Rect(0f, 0f, 2 * r, h), 90f, 180f, false)
            close()
        }
        return Outline.Generic(path)
    }
}
