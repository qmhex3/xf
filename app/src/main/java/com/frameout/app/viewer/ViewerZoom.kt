package com.frameout.app.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.frameout.core.edit.Transform
import com.frameout.core.edit.ViewGeometry
import com.frameout.core.util.EditStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val ZOOM_STEP = 2f

@Composable
internal fun PixelGrid(geo: ViewGeometry, t: Transform) {
    if (geo.pixelSize(t) < ViewGeometry.GRID_MIN_CELL) return
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val left = (geo.toScreenX(0f, t) + cx).coerceIn(0f, size.width)
        val right = (geo.toScreenX(1f, t) + cx).coerceIn(0f, size.width)
        val top = (geo.toScreenY(0f, t) + cy).coerceIn(0f, size.height)
        val bottom = (geo.toScreenY(1f, t) + cy).coerceIn(0f, size.height)
        val firstX = floor(geo.toImageX(left - cx, t)).toInt().coerceAtLeast(0)
        val lastX = ceil(geo.toImageX(right - cx, t)).toInt().coerceAtMost(geo.imageW)
        val firstY = floor(geo.toImageY(top - cy, t)).toInt().coerceAtLeast(0)
        val lastY = ceil(geo.toImageY(bottom - cy, t)).toInt().coerceAtMost(geo.imageH)
        for (x in firstX..lastX) {
            val sx = floor(geo.toScreenX(x / geo.imageW.toFloat(), t) + cx) + 0.5f
            drawGridLine(Offset(sx, top), Offset(sx, bottom))
        }
        for (y in firstY..lastY) {
            val sy = floor(geo.toScreenY(y / geo.imageH.toFloat(), t) + cy) + 0.5f
            drawGridLine(Offset(left, sy), Offset(right, sy))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridLine(from: Offset, to: Offset) {
    drawLine(Color.Black.copy(alpha = 0.35f), from, to, 1f)
    drawLine(Color.White.copy(alpha = 0.28f), from + Offset(1f, 1f), to + Offset(1f, 1f), 1f)
}

@Composable
internal fun ZoomHud(geo: ViewGeometry, t: Transform, gridOn: Boolean, onGrid: () -> Unit, onTransform: (Transform) -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(shape = FrameShapes.xl, modifier = modifier) {
        Column(Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
            HudButton(FrameIconsExtra.Plus, EditStrings.zoomIn()) { onTransform(geo.zoomed(t, ZOOM_STEP, 0f, 0f)) }
            HudReadout(geo.pixelSize(t)) { onTransform(geo.zoomed(t, geo.actualPixels() / t.scale, 0f, 0f)) }
            HudButton(FrameIconsExtra.Minus, EditStrings.zoomOut()) { onTransform(geo.zoomed(t, 1f / ZOOM_STEP, 0f, 0f)) }
            HudButton(FrameIconsExtra.Fit, EditStrings.zoomFit()) { onTransform(Transform.Identity) }
            HudButton(FrameIconsExtra.Pixelate, EditStrings.pixelGrid(), gridOn, onGrid)
        }
    }
}

@Composable
private fun HudButton(icon: Int, label: String, active: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).frameClickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(if (active) FrameTheme.colors.primary else FrameTheme.colors.onSurface)
        )
    }
}

@Composable
private fun HudReadout(pixelSize: Float, onClick: () -> Unit) {
    val percent = (pixelSize * 100f).roundToInt()
    Box(Modifier.size(width = 44.dp, height = 36.dp).frameClickable(onClickLabel = EditStrings.zoomActual(), onClick = onClick), contentAlignment = Alignment.Center) {
        FrameText("$percent%", style = FrameTheme.typography.monoSmall, color = FrameTheme.colors.primary)
    }
}
