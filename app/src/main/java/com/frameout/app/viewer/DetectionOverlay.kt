package com.frameout.app.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.quality.TagGroup
import com.frameout.core.quality.TagNames
import com.frameout.designsystem.theme.FrameTheme

private const val ACCENT_LIFT = 0.3f

private class DetectionPalette(val colors: Map<TagGroup, Color>, val ink: Color)

@Composable
private fun rememberDetectionPalette(): DetectionPalette {
    val theme = FrameTheme.colors
    val accent = FrameTheme.accent
    return remember(theme, accent) {
        fun lift(c: Color) = lerp(c, Color.White, ACCENT_LIFT)
        DetectionPalette(
            mapOf(
                TagGroup.People to theme.onDarkAccent,
                TagGroup.Things to lift(theme.accentA),
                TagGroup.Categories to lift(theme.accentB),
                TagGroup.Text to lift(theme.accentC),
                TagGroup.Codes to lift(theme.success)
            ),
            accent.darkOnPrimary
        )
    }
}

@Composable
internal fun DetectionOverlay(
    detections: List<Detection>,
    imageWidth: Int,
    imageHeight: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    if (detections.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return
    val palette = rememberDetectionPalette()
    val lang = com.frameout.core.util.AppLang.of(LocalConfiguration.current.locales[0].language)
    val measurer = rememberTextMeasurer()
    Canvas(modifier.fillMaxSize()) {
        val fit = minOf(size.width / imageWidth, size.height / imageHeight)
        val dw = imageWidth * fit
        val dh = imageHeight * fit
        val ox = (size.width - dw) / 2f
        val oy = (size.height - dh) / 2f
        fun px(u: Float) = size.width / 2f + (ox + u * dw - size.width / 2f) * scale + offsetX
        fun py(v: Float) = size.height / 2f + (oy + v * dh - size.height / 2f) * scale + offsetY
        val stroke = 2.dp.toPx()
        val radius = CornerRadius(6.dp.toPx())
        val padX = 6.dp.toPx()
        val padY = 3.dp.toPx()
        val style = TextStyle(color = palette.ink, fontSize = 11.sp)
        detections.forEach { d ->
            val color = palette.colors.getValue(TagNames.group(d.tags.first()))
            val left = px(d.box.l)
            val top = py(d.box.t)
            val boxW = (px(d.box.r) - left).coerceAtLeast(1f)
            val boxH = (py(d.box.b) - top).coerceAtLeast(1f)
            drawRoundRect(color, Offset(left, top), Size(boxW, boxH), radius, Stroke(stroke))
            val label = d.tags.joinToString(" · ") { TagNames.display(it, lang) }
            val text = measurer.measure(label, style, overflow = TextOverflow.Ellipsis, maxLines = 1, constraints = androidx.compose.ui.unit.Constraints(maxWidth = boxW.toInt().coerceAtLeast(60.dp.roundToPx())))
            val chipW = text.size.width + padX * 2
            val chipH = text.size.height + padY * 2
            val chipTop = if (top - chipH >= 0f) top - chipH else top
            drawRoundRect(color, Offset(left, chipTop), Size(chipW, chipH), CornerRadius(5.dp.toPx()))
            drawText(text, topLeft = Offset(left + padX, chipTop + padY))
        }
    }
}
