package com.frameout.app.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun LiveTextOverlay(
    state: LiveTextState,
    imageWidth: Int,
    imageHeight: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    if (!state.active || state.words.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return
    val accent = FrameTheme.colors.primary
    Canvas(modifier.fillMaxSize()) {
        val fit = minOf(size.width / imageWidth, size.height / imageHeight)
        val dw = imageWidth * fit
        val dh = imageHeight * fit
        val ox = (size.width - dw) / 2f
        val oy = (size.height - dh) / 2f
        fun px(u: Float) = size.width / 2f + (ox + u * dw - size.width / 2f) * scale + offsetX
        fun py(v: Float) = size.height / 2f + (oy + v * dh - size.height / 2f) * scale + offsetY
        val pad = 1.5.dp.toPx()
        val radius = CornerRadius(4.dp.toPx())
        val hairline = 1.dp.toPx()
        state.words.forEachIndexed { i, word ->
            val left = px(word.box.l) - pad
            val top = py(word.box.t) - pad
            val right = px(word.box.r) + pad
            val bottom = py(word.box.b) + pad
            if (right < 0f || bottom < 0f || left > size.width || top > size.height) return@forEachIndexed
            val topLeft = Offset(left, top)
            val box = Size((right - left).coerceAtLeast(1f), (bottom - top).coerceAtLeast(1f))
            if (state.isSelected(i)) {
                drawRoundRect(accent.copy(alpha = 0.38f), topLeft, box, radius)
                drawRoundRect(accent.copy(alpha = 0.9f), topLeft, box, radius, Stroke(hairline * 1.5f))
            } else {
                drawRoundRect(Color.White.copy(alpha = 0.10f), topLeft, box, radius)
                drawRoundRect(Color.White.copy(alpha = 0.45f), topLeft, box, radius, Stroke(hairline))
            }
        }
    }
}
