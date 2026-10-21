package com.frameout.app.billing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private const val HANDLE_ANGLE = 0.785f

@Composable
internal fun rememberCycle(reduce: Boolean, ms: Int, still: Float): State<Float> {
    val t = remember { Animatable(if (reduce) still else 0f) }
    LaunchedEffect(reduce) {
        if (reduce) return@LaunchedEffect
        while (true) {
            t.snapTo(0f)
            t.animateTo(1f, tween(ms, easing = LinearEasing))
        }
    }
    return t.asState()
}

internal fun phase(p: Float, from: Float, to: Float): Float {
    val x = ((p - from) / (to - from)).coerceIn(0f, 1f)
    val tail = ((0.96f - p) / 0.06f).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x) * tail
}

internal fun activeTool(p: Float): Int = when {
    p in 0.06f..0.32f -> 0
    p in 0.36f..0.60f -> 1
    p in 0.64f..0.90f -> 2
    else -> -1
}

internal fun DrawScope.drawBlurLens(scene: ShowcaseScene, k: Float, accent: Color) {
    if (k <= 0f) return
    val c = Offset(size.width * SceneZones.face.x, size.height * SceneZones.face.y)
    val r = size.height * SceneZones.FACE_R * 1.55f * k
    clipPath(Path().apply { addOval(Rect(c, r)) }) { drawImage(scene.soft) }
    drawCircle(Color.White.copy(alpha = 0.18f * k), r, c)
    drawCircle(Color.White.copy(alpha = 0.75f * k), r, c, style = Stroke(1.5.dp.toPx()))
    val handle = Offset(c.x + r * cos(HANDLE_ANGLE), c.y + r * sin(HANDLE_ANGLE))
    drawCircle(accent, 5.dp.toPx() * k, handle)
    drawCircle(Color.White, 2.dp.toPx() * k, handle)
}

internal fun DrawScope.drawPixelPlate(scene: ShowcaseScene, k: Float, accent: Color) {
    if (k <= 0f) return
    val z = SceneZones.plate
    val tl = Offset(size.width * z.left, size.height * z.top)
    val sz = Size(size.width * z.width, size.height * z.height)
    val pad = 4.dp.toPx()
    val reveal = sz.width * k
    clipRect(tl.x - pad, tl.y - pad, tl.x + reveal + pad, tl.y + sz.height + pad) { drawImage(scene.blocky) }
    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))
    val box = Size(sz.width + pad * 2, sz.height + pad * 2)
    drawRoundRect(accent.copy(alpha = 0.95f * k), tl - Offset(pad, pad), box, CornerRadius(10.dp.toPx()), style = Stroke(1.6.dp.toPx(), pathEffect = dash))
    drawLine(Color.White.copy(alpha = 0.9f * (1f - k)), Offset(tl.x + reveal, tl.y - pad), Offset(tl.x + reveal, tl.y + sz.height + pad), 2.dp.toPx())
}

internal fun DrawScope.drawCaption(measurer: TextMeasurer, style: TextStyle, text: String, k: Float, accent: Color) {
    if (k <= 0.01f) return
    val shown = text.take((text.length * k).toInt().coerceIn(1, text.length))
    val layout = measurer.measure(shown, style.copy(color = Color.White.copy(alpha = k)))
    val padX = 10.dp.toPx()
    val padY = 4.dp.toPx()
    val box = Size(layout.size.width + padX * 2, layout.size.height + padY * 2)
    val tl = Offset(size.width - box.width - 10.dp.toPx(), 10.dp.toPx())
    drawRoundRect(Color.Black.copy(alpha = 0.45f * k), tl, box, CornerRadius(11.dp.toPx()))
    drawRoundRect(accent.copy(alpha = 0.9f * k), tl, box, CornerRadius(11.dp.toPx()), style = Stroke(1.dp.toPx()))
    drawText(layout, topLeft = Offset(tl.x + padX, tl.y + padY))
}

@Composable
internal fun ToolChip(icon: Int, on: Boolean, accent: Color) {
    val bg = if (on) accent else Color.Black.copy(alpha = 0.5f)
    val tint = if (on) proOnBrush() else Color.White.copy(alpha = 0.85f)
    val ring = Color.White.copy(alpha = if (on) 0.6f else 0.18f)
    Box(Modifier.size(28.dp).background(bg, CircleShape).border(1.dp, ring, CircleShape), Alignment.Center) {
        Image(painterResource(icon), null, Modifier.size(15.dp), colorFilter = ColorFilter.tint(tint))
    }
}
