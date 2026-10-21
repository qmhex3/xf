package com.frameout.app.viewer

import androidx.compose.ui.geometry.Offset
import com.frameout.core.quality.NormBox
import com.frameout.core.quality.ScannedWord
import kotlin.math.max
import kotlin.math.min

internal object LiveTextGeometry {
    fun toImage(point: Offset, viewW: Float, viewH: Float, imgW: Int, imgH: Int, scale: Float, offX: Float, offY: Float): Offset {
        val fit = min(viewW / imgW, viewH / imgH)
        val dw = imgW * fit
        val dh = imgH * fit
        val ox = (viewW - dw) / 2f
        val oy = (viewH - dh) / 2f
        val s = if (scale <= 0f) 1f else scale
        return Offset(
            ((point.x - viewW / 2f - offX) / s + viewW / 2f - ox) / dw,
            ((point.y - viewH / 2f - offY) / s + viewH / 2f - oy) / dh
        )
    }

    fun distance(box: NormBox, p: Offset, dw: Float, dh: Float): Float {
        val dx = max(max(box.l - p.x, p.x - box.r), 0f) * dw
        val dy = max(max(box.t - p.y, p.y - box.b), 0f) * dh
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun nearest(
        words: List<ScannedWord>, point: Offset, viewW: Float, viewH: Float, imgW: Int, imgH: Int,
        scale: Float, offX: Float, offY: Float, maxDistancePx: Float = Float.MAX_VALUE
    ): Int {
        if (words.isEmpty() || imgW <= 0 || imgH <= 0 || viewW <= 0f || viewH <= 0f) return -1
        val fit = min(viewW / imgW, viewH / imgH)
        val dw = imgW * fit
        val dh = imgH * fit
        val p = toImage(point, viewW, viewH, imgW, imgH, scale, offX, offY)
        val limit = maxDistancePx / (if (scale <= 0f) 1f else scale)
        var best = -1
        var bestDist = Float.MAX_VALUE
        var bestArea = Float.MAX_VALUE
        words.forEachIndexed { i, w ->
            val d = distance(w.box, p, dw, dh)
            val area = w.box.width * w.box.height
            if (d <= limit && (d < bestDist || (d == bestDist && area < bestArea))) {
                best = i; bestDist = d; bestArea = area
            }
        }
        return best
    }
}
