package com.frameout.app.billing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.theme.FrameTheme

private const val FADE_START = 0.86f
private const val CONTENT_SCALE = 0.86f
private val SkyTop = Color(0xFFF7A76D)
private val SkyMid = Color(0xFF8A5CB3)
private val SkyBottom = Color(0xFF2A3F78)
private val Sun = Color(0xFFFFE9A8)
private val HillFar = Color(0xFF15213F)
private val HillNear = Color(0xFF0F1A36)

@Composable
internal fun ShowcaseStage(modifier: Modifier = Modifier, decorative: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    rememberShowcaseScene()
    val colors = FrameTheme.colors
    val strength = if (colors.isDark) 0.40f else 0.22f
    Box(
        modifier
            .fillMaxWidth()
            .height(188.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(FADE_START to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
            }
            .drawBehind {
                val w = size.width
                val h = size.height
                drawRect(Brush.radialGradient(listOf(colors.accentA.copy(alpha = strength), Color.Transparent), Offset(w / 2f, 0f), w * 0.75f))
                drawRect(Brush.radialGradient(listOf(colors.accentC.copy(alpha = strength * 0.65f), Color.Transparent), Offset(w * 0.15f, h * 0.7f), w * 0.45f))
                drawRect(Brush.radialGradient(listOf(colors.accentB.copy(alpha = strength * 0.65f), Color.Transparent), Offset(w * 0.88f, h * 0.7f), w * 0.45f))
            }
            .then(if (decorative) Modifier.clearAndSetSemantics { } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.graphicsLayer { scaleX = CONTENT_SCALE; scaleY = CONTENT_SCALE }, Alignment.Center, content = content)
    }
}

internal fun DrawScope.drawPhoto(filter: ColorFilter?, sample: ImageBitmap? = null) {
    ShowcaseSceneCache.current?.let { return drawCover(it.sharp, filter) }
    if (sample != null) return drawCover(sample, filter)
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(SkyTop, SkyMid, SkyBottom)), colorFilter = filter)
    drawCircle(Sun, radius = h * 0.11f, center = Offset(w * 0.72f, h * 0.3f), colorFilter = filter)
    drawOval(HillFar, Offset(-w * 0.15f, h * 0.72f), Size(w * 0.9f, h * 0.55f), colorFilter = filter)
    drawOval(HillNear, Offset(w * 0.3f, h * 0.78f), Size(w * 0.9f, h * 0.5f), colorFilter = filter)
}

internal fun DrawScope.drawCover(sample: ImageBitmap, filter: ColorFilter?) {
    val ratio = size.width / size.height
    val wide = sample.width.toFloat() / sample.height > ratio
    val cropW = if (wide) (sample.height * ratio).toInt() else sample.width
    val cropH = if (wide) sample.height else (sample.width / ratio).toInt()
    drawImage(
        sample,
        srcOffset = IntOffset((sample.width - cropW) / 2, (sample.height - cropH) / 2),
        srcSize = IntSize(cropW, cropH),
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        colorFilter = filter
    )
}

val LocalPaywallSample = androidx.compose.runtime.staticCompositionLocalOf<ImageBitmap?> { null }

internal fun Modifier.fadedEnd(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val rtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
        val stops = if (rtl) arrayOf(0f to Color.Transparent, 0.1f to Color.Black, 1f to Color.Black) else arrayOf(0f to Color.Black, 0.9f to Color.Black, 1f to Color.Transparent)
        drawRect(Brush.horizontalGradient(*stops), blendMode = BlendMode.DstIn)
    }
