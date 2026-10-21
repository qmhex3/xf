package com.frameout.app.trim

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
internal class SnapshotFxState {
    var shutter by mutableStateOf(0)
        private set
    var preview by mutableStateOf<Bitmap?>(null)
        private set
    var previewSerial by mutableStateOf(0)
        private set

    fun fire() {
        shutter += 1
    }

    fun show(bitmap: Bitmap) {
        preview = bitmap
        previewSerial += 1
    }
}

@Composable
internal fun BoxScope.SnapshotOverlay(fx: SnapshotFxState) {
    val flash = remember { Animatable(0f) }
    val focus = remember { Animatable(1f) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(fx.shutter) {
        if (fx.shutter == 0) return@LaunchedEffect
        flash.snapTo(0f)
        focus.snapTo(0f)
        launch { focus.animateTo(1f, tween(560, easing = FastOutSlowInEasing)) }
        flash.animateTo(0.88f, tween(45, easing = LinearEasing))
        flash.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(fx.previewSerial) {
        if (fx.previewSerial == 0) return@LaunchedEffect
        shown = true
        delay(PREVIEW_HOLD_MS)
        shown = false
    }
    Canvas(Modifier.fillMaxSize()) {
        val glow = flash.value
        if (glow > 0f) drawRect(Color.White.copy(alpha = glow))
        val progress = focus.value
        if (progress < 1f) drawFocusBrackets(progress)
    }
    val bitmap = fx.preview
    AnimatedVisibility(
        visible = shown && bitmap != null,
        modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        enter = fadeIn(tween(160)) +
            scaleIn(tween(280, easing = FastOutSlowInEasing), initialScale = 0.6f, transformOrigin = TransformOrigin(0f, 1f)) +
            slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 2 },
        exit = fadeOut(tween(240)) + slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it }
    ) {
        bitmap?.let { SnapshotThumb(it) }
    }
}

@Composable
private fun SnapshotThumb(bitmap: Bitmap) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val aspect = (image.width.toFloat() / image.height.coerceAtLeast(1)).coerceIn(0.4f, 2.5f)
    val shape = RoundedCornerShape(10.dp)
    val size = if (aspect >= 1f) Modifier.width(THUMB_EDGE) else Modifier.height(THUMB_EDGE)
    Image(
        image,
        null,
        size.aspectRatio(aspect).shadow(10.dp, shape).clip(shape).border(2.dp, Color.White, shape),
        contentScale = ContentScale.Crop
    )
}

private fun DrawScope.drawFocusBrackets(progress: Float) {
    val eased = FastOutSlowInEasing.transform(progress)
    val alpha = (if (progress < 0.18f) progress / 0.18f else 1f - (progress - 0.18f) / 0.82f).coerceIn(0f, 1f)
    val inset = lerp(6.dp.toPx(), 24.dp.toPx(), eased)
    val arm = (minOf(size.width, size.height) * 0.14f).coerceIn(18.dp.toPx(), 44.dp.toPx())
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset
    val segments = listOf(
        Offset(left, top) to Offset(left + arm, top),
        Offset(left, top) to Offset(left, top + arm),
        Offset(right, top) to Offset(right - arm, top),
        Offset(right, top) to Offset(right, top + arm),
        Offset(left, bottom) to Offset(left + arm, bottom),
        Offset(left, bottom) to Offset(left, bottom - arm),
        Offset(right, bottom) to Offset(right - arm, bottom),
        Offset(right, bottom) to Offset(right, bottom - arm)
    )
    segments.forEach { (from, to) ->
        drawLine(Color.Black.copy(alpha = alpha * 0.35f), from, to, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
    }
    segments.forEach { (from, to) ->
        drawLine(Color.White.copy(alpha = alpha), from, to, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    }
}

private const val PREVIEW_HOLD_MS = 1600L
private val THUMB_EDGE = 96.dp
