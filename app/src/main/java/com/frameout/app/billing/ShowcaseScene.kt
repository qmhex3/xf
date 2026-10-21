package com.frameout.app.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

internal const val SCENE_W_DP = 248
internal const val SCENE_H_DP = 150
private const val SOFT_DIVISOR = 9
private const val BLOCK_DIVISOR = 16

internal object SceneZones {
    val face = Offset(0.27f, 0.50f)
    const val FACE_R = 0.115f
    val plate = Rect(0.55f, 0.66f, 0.90f, 0.84f)
}

internal class ShowcaseScene(
    val sharp: ImageBitmap,
    val soft: ImageBitmap,
    val blocky: ImageBitmap,
    val width: Int,
    val height: Int,
    val source: ImageBitmap?
) {
    fun matches(w: Int, h: Int, sample: ImageBitmap?): Boolean = width == w && height == h && source === sample
}

internal object ShowcaseSceneCache {
    var current: ShowcaseScene? by mutableStateOf(null)
        private set

    fun find(w: Int, h: Int, sample: ImageBitmap?): ShowcaseScene? = current?.takeIf { it.matches(w, h, sample) }

    fun store(scene: ShowcaseScene) {
        current = scene
    }

    fun clear() {
        current = null
    }
}

@Composable
internal fun rememberShowcaseScene(): ShowcaseScene? {
    val density = LocalDensity.current
    val sample = LocalPaywallSample.current
    val w = with(density) { SCENE_W_DP.dp.roundToPx() }
    val h = with(density) { SCENE_H_DP.dp.roundToPx() }
    val ready = ShowcaseSceneCache.find(w, h, sample)
    return produceState(ready, w, h, sample) {
        if (value != null) return@produceState
        val built = withContext(Dispatchers.Default) { buildScene(w, h, density, sample) }
        ShowcaseSceneCache.store(built)
        value = built
    }.value
}

private fun buildScene(w: Int, h: Int, density: Density, sample: ImageBitmap?): ShowcaseScene {
    val sharp = render(w, h, density) { if (sample != null) drawCover(sample, null) else drawSceneArt() }
    val soft = lowPass(sharp, w, h, SOFT_DIVISOR, FilterQuality.High)
    val tiny = lowPass(sharp, w, h, BLOCK_DIVISOR, FilterQuality.Medium)
    return ShowcaseScene(sharp, soft, tiny, w, h, sample)
}

private fun render(w: Int, h: Int, density: Density, block: DrawScope.() -> Unit): ImageBitmap {
    val bitmap = ImageBitmap(w, h)
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap), Size(w.toFloat(), h.toFloat()), block)
    return bitmap
}

private fun lowPass(src: ImageBitmap, w: Int, h: Int, divisor: Int, quality: FilterQuality): ImageBitmap {
    val sw = max(2, w / divisor)
    val sh = max(2, h / divisor)
    val small = render(sw, sh, Density(1f)) { drawImage(src, dstSize = IntSize(sw, sh), filterQuality = FilterQuality.Medium) }
    return render(w, h, Density(1f)) { drawImage(small, dstSize = IntSize(w, h), filterQuality = quality) }
}
