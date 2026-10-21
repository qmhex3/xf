package com.frameout.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private const val BrandDeepen = 0.34f

object FrameGradients {
    val Brand: Brush
        @Composable get() {
            val c = FrameTheme.colors
            return remember(c.accentA, c.accentB) {
                Brush.horizontalGradient(listOf(lerp(c.accentA, Color.Black, BrandDeepen), lerp(c.accentB, Color.Black, BrandDeepen)))
            }
        }

    val Progress: Brush
        @Composable get() {
            val c = FrameTheme.colors
            return remember(c.accentA, c.accentC) { Brush.horizontalGradient(listOf(c.accentA, c.accentC)) }
        }

    val Fab: Brush
        @Composable get() {
            val c = FrameTheme.colors
            return remember(c.accentA, c.accentB, c.accentC) {
                Brush.linearGradient(listOf(c.accentC, c.accentA, c.accentB))
            }
        }
}

@Composable
fun frameHairline(): Color =
    if (FrameTheme.colors.isDark) Color.White.copy(alpha = 0.13f) else Color.Black.copy(alpha = 0.08f)

fun Modifier.frameAmbientGlow(): Modifier = composed {
    val colors = FrameTheme.colors
    val strength = if (colors.isDark) 1f else 0.55f
    Modifier.drawWithCache {
        val radius = size.width * 0.85f
        fun blob(color: Color, alpha: Float, center: Offset): Pair<Brush, Offset> =
            Brush.radialGradient(listOf(color.copy(alpha = alpha * strength), Color.Transparent), center, radius) to center
        val blobs = listOf(
            blob(colors.accentA, 0.30f, Offset(size.width, 0f)),
            blob(colors.accentC, 0.20f, Offset(0f, size.height * 0.35f)),
            blob(colors.accentB, 0.24f, Offset(size.width, size.height * 0.9f))
        )
        onDrawBehind { blobs.forEach { (brush, center) -> drawCircle(brush, radius, center) } }
    }
}

private const val CardTopAlpha = 0.66f
private const val CardBottomAlpha = 0.52f

@Composable
fun frameCardBrush(tint: Color = Color.Transparent, tintAmount: Float = 0f): Brush {
    val colors = FrameTheme.colors
    val top: Color
    val bottom: Color
    if (colors.isDark) {
        top = lerp(colors.container, Color.White, 0.05f).copy(alpha = CardTopAlpha)
        bottom = colors.container.copy(alpha = CardBottomAlpha)
    } else {
        top = colors.surface
        bottom = lerp(colors.surface, colors.background, 0.65f)
    }
    val amount = tintAmount.coerceIn(0f, 1f)
    return Brush.linearGradient(listOf(lerp(top, tint.copy(alpha = top.alpha), amount), bottom))
}

@Composable
fun frameCardEdge(accent: Color = Color.Unspecified, accentAlpha: Float = 0f): Brush {
    val dark = FrameTheme.colors.isDark
    val hi = if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.09f)
    val lo = if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
    return if (accent == Color.Unspecified) Brush.verticalGradient(listOf(hi, lo))
    else Brush.verticalGradient(listOf(accent.copy(alpha = accentAlpha), accent.copy(alpha = accentAlpha * 0.35f)))
}
