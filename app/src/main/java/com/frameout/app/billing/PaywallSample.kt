package com.frameout.app.billing

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val SAMPLE_PX = 720

object PaywallSample {
    var image: ImageBitmap? by mutableStateOf(null)
}

@Composable
fun PaywallSampleEffect(source: Bitmap?) {
    LaunchedEffect(source) {
        PaywallSample.image = if (source == null) null else withContext(Dispatchers.Default) { sampleOf(source) }
    }
    DisposableEffect(Unit) { onDispose {
        PaywallSample.image = null
        ShowcaseSceneCache.clear()
    } }
}

private fun sampleOf(src: Bitmap): ImageBitmap? = runCatching {
    if (src.isRecycled) return@runCatching null
    val scale = minOf(1f, SAMPLE_PX.toFloat() / maxOf(src.width, src.height))
    val w = (src.width * scale).roundToInt().coerceAtLeast(1)
    val h = (src.height * scale).roundToInt().coerceAtLeast(1)
    Bitmap.createScaledBitmap(src, w, h, true).asImageBitmap()
}.getOrNull()
