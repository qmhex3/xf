package com.frameout.app.billing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val LOCKED_ALPHA = 0.82f

@Composable
fun LockGlyph(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier.size(11.dp)) {
        val s = size.minDimension
        val shackle = Path().apply {
            moveTo(s * 0.3f, s * 0.5f)
            lineTo(s * 0.3f, s * 0.33f)
            arcTo(Rect(s * 0.3f, s * 0.08f, s * 0.7f, s * 0.58f), 180f, 180f, false)
            lineTo(s * 0.7f, s * 0.5f)
        }
        drawPath(shackle, tint, style = Stroke(s * 0.125f, cap = StrokeCap.Round))
        drawRoundRect(tint, Offset(s * 0.18f, s * 0.45f), Size(s * 0.64f, s * 0.46f), CornerRadius(s * 0.14f))
    }
}

@Composable
fun ProLockDot(modifier: Modifier = Modifier) {
    Box(
        modifier.size(20.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(7.dp)),
        Alignment.Center
    ) { LockGlyph() }
}

@Composable
fun BoxScope.ProCorner(locked: Boolean, phase: Int = 0, compact: Boolean = false) {
    if (!locked) return
    val inset = if (compact) 3.dp else 6.dp
    ProLockDot(Modifier.align(Alignment.TopStart).padding(inset))
    ProBadge(Modifier.align(Alignment.TopEnd).padding(inset), phase, compact)
}

@Composable
fun BoxScope.ProLockCorner(locked: Boolean) {
    if (!locked) return
    ProLockDot(Modifier.align(Alignment.TopEnd).padding(2.dp))
}

fun Modifier.proDim(locked: Boolean): Modifier = if (locked) alpha(LOCKED_ALPHA) else this
