package com.frameout.core.quality

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PxRect(val x: Int, val y: Int, val w: Int, val h: Int) {
    val r: Int get() = x + w
    val b: Int get() = y + h
    val area: Long get() = w.toLong() * h
    val minSide: Int get() = min(w, h)
}

object SmartZoom {
    const val OVERLAP = 0.25f
    const val MIN_TILE = 160
    const val TILE_TARGET = 640
    const val MAX_UPSCALE = 3f
    const val FACE_ZOOM_TARGET = 320
    const val FACE_ZOOM_MAX_UPSCALE = 4f
    const val FACE_PAD = 0.5f
    const val DETECTABLE_PX = 100
    const val CONFIRM_IOU = 0.35f
    const val EDGE_PX = 3
    const val TILE_STRONG = 0.78f
    const val TILE_PAIR = 0.62f
    const val MIN_ASPECT = 0.55f
    const val MAX_ASPECT = 1.8f

    fun grid(width: Int, height: Int, cols: Int = 2, rows: Int = 2, overlap: Float = OVERLAP): List<PxRect> {
        if (cols < 1 || rows < 1 || width <= 0 || height <= 0) return emptyList()
        val tw = ceil(width / (cols - (cols - 1) * overlap)).toInt().coerceAtMost(width)
        val th = ceil(height / (rows - (rows - 1) * overlap)).toInt().coerceAtMost(height)
        if (tw < MIN_TILE || th < MIN_TILE) return emptyList()
        if (tw >= width && th >= height) return emptyList()
        val xs = positions(width, tw, cols)
        val ys = positions(height, th, rows)
        return ys.flatMap { y -> xs.map { x -> PxRect(x, y, tw, th) } }
    }

    private fun positions(total: Int, size: Int, count: Int): List<Int> =
        if (count == 1) listOf(0) else (0 until count).map { (it * (total - size).toFloat() / (count - 1)).roundToInt() }

    fun scaleFor(width: Int, height: Int, target: Int = TILE_TARGET, maxScale: Float = MAX_UPSCALE): Float {
        val long = max(width, height)
        if (long <= 0 || long >= target) return 1f
        return (target.toFloat() / long).coerceIn(1f, maxScale)
    }

    fun zoomRect(box: PxRect, width: Int, height: Int, pad: Float = FACE_PAD): PxRect? {
        if (width <= 0 || height <= 0 || box.w <= 0 || box.h <= 0) return null
        val side = (max(box.w, box.h) * (1f + 2f * pad)).roundToInt().coerceAtMost(min(width, height))
        if (side < 16) return null
        val cx = (box.x + box.r) / 2
        val cy = (box.y + box.b) / 2
        val x = (cx - side / 2).coerceIn(0, width - side)
        val y = (cy - side / 2).coerceIn(0, height - side)
        return PxRect(x, y, side, side)
    }

    fun cutByTile(box: PxRect, tile: PxRect, width: Int, height: Int): Boolean =
        (tile.x > 0 && box.x - tile.x <= EDGE_PX) ||
            (tile.r < width && tile.r - box.r <= EDGE_PX) ||
            (tile.y > 0 && box.y - tile.y <= EDGE_PX) ||
            (tile.b < height && tile.b - box.b <= EDGE_PX)

    fun plausibleFace(box: PxRect): Boolean {
        if (box.w <= 0 || box.h <= 0) return false
        val aspect = box.w.toFloat() / box.h
        return aspect in MIN_ASPECT..MAX_ASPECT
    }

    fun iou(a: PxRect, b: PxRect): Float {
        val inter = intersection(a, b)
        if (inter <= 0L) return 0f
        return inter.toFloat() / (a.area + b.area - inter)
    }

    fun containment(a: PxRect, b: PxRect): Float {
        val inter = intersection(a, b)
        val smaller = min(a.area, b.area)
        return if (inter <= 0L || smaller <= 0L) 0f else inter.toFloat() / smaller
    }

    private fun intersection(a: PxRect, b: PxRect): Long {
        val w = min(a.r, b.r) - max(a.x, b.x)
        val h = min(a.b, b.b) - max(a.y, b.y)
        return if (w <= 0 || h <= 0) 0L else w.toLong() * h
    }

    fun <T> suppress(items: List<T>, rect: (T) -> PxRect, iouLimit: Float = 0.4f, containLimit: Float = 0.7f): List<T> {
        val out = ArrayList<T>()
        for (item in items.sortedByDescending { rect(it).area }) {
            val r = rect(item)
            if (out.none { val o = rect(it); iou(o, r) >= iouLimit || containment(o, r) >= containLimit }) out += item
        }
        return out
    }

    fun vote(whole: Float?, tiles: List<Float>, single: Float = TILE_STRONG, pair: Float = TILE_PAIR): Float? {
        val sorted = tiles.sortedDescending()
        val tile: Float? = when {
            sorted.isNotEmpty() && sorted[0] >= single -> sorted[0]
            sorted.size >= 2 && sorted[1] >= pair -> (sorted[0] + sorted[1]) / 2f
            else -> null
        }
        return when {
            whole != null && tile != null -> min(1f, max(whole, tile) + 0.05f)
            whole != null -> whole
            else -> tile
        }
    }
}
