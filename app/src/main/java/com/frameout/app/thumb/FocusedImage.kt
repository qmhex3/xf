package com.frameout.app.thumb

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.frameout.core.quality.FocusRegion
import com.frameout.core.quality.NormBox
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun FocusedImage(bitmap: Bitmap, focus: NormBox, modifier: Modifier = Modifier, fit: Boolean = false) {
    if (bitmap.isRecycled) return
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Canvas(modifier) {
        val dw = size.width
        val dh = size.height
        if (dw <= 0f || dh <= 0f) return@Canvas
        val region = FocusRegion.pixels(focus, image.width, image.height, if (fit) null else dw / dh)
        val dst = if (fit) {
            val s = min(dw / region.w, dh / region.h)
            IntSize((region.w * s).roundToInt().coerceAtLeast(1), (region.h * s).roundToInt().coerceAtLeast(1))
        } else {
            IntSize(dw.roundToInt(), dh.roundToInt())
        }
        drawImage(
            image,
            srcOffset = IntOffset(region.x, region.y),
            srcSize = IntSize(region.w, region.h),
            dstOffset = IntOffset(((dw - dst.width) / 2f).roundToInt(), ((dh - dst.height) / 2f).roundToInt()),
            dstSize = dst,
            filterQuality = FilterQuality.Medium
        )
    }
}
