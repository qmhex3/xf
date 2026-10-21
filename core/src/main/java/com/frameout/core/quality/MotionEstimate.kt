package com.frameout.core.quality

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

object MotionEstimate {
    private const val MAX_SHIFT = 0.10
    private const val MIN_SIDE = 16
    private const val MAX_RESIDUAL = 0.8
    private const val FULL_PENALTY_SPEED = 0.03f
    private const val MAX_PENALTY = 0.30f

    class Profile(val cols: FloatArray, val rows: FloatArray)

    fun profile(luma: IntArray, width: Int, height: Int): Profile? {
        if (width < MIN_SIDE || height < MIN_SIDE || luma.size < width * height) return null
        val cols = FloatArray(width)
        val rows = FloatArray(height)
        for (y in 0 until height) {
            var rowSum = 0L
            val base = y * width
            for (x in 0 until width) {
                val v = luma[base + x]
                cols[x] += v.toFloat()
                rowSum += v
            }
            rows[y] = rowSum.toFloat() / width
        }
        for (x in 0 until width) cols[x] /= height
        return Profile(cols, rows)
    }

    fun speed(a: Profile, b: Profile): Float? {
        if (a.cols.size != b.cols.size || a.rows.size != b.rows.size) return null
        val dx = bestShift(a.cols, b.cols) ?: return null
        val dy = bestShift(a.rows, b.rows) ?: return null
        return (hypot(dx, dy) / max(a.cols.size, a.rows.size)).toFloat()
    }

    fun speeds(profiles: List<Profile?>): FloatArray {
        val links = FloatArray(max(0, profiles.size - 1)) { i ->
            val a = profiles[i]
            val b = profiles[i + 1]
            if (a == null || b == null) 0f else speed(a, b) ?: 0f
        }
        return FloatArray(profiles.size) { i ->
            val before = if (i > 0) links[i - 1] else 0f
            val after = if (i < links.size) links[i] else 0f
            max(before, after)
        }
    }

    fun factor(speed: Float): Float = 1f - MAX_PENALTY * (speed / FULL_PENALTY_SPEED).coerceIn(0f, 1f)

    private fun bestShift(a: FloatArray, b: FloatArray): Double? {
        val n = a.size
        val range = a.max() - a.min()
        if (range < 2f) return 0.0
        val reach = max(2, (n * MAX_SHIFT).toInt())
        val sad = DoubleArray(2 * reach + 1)
        for (s in -reach..reach) {
            var total = 0.0
            var count = 0
            for (i in reach until n - reach) {
                total += abs(a[i] - b[i + s])
                count++
            }
            sad[s + reach] = if (count == 0) Double.MAX_VALUE else total / count
        }
        var best = 0
        for (i in sad.indices) if (sad[i] < sad[best]) best = i
        val spread = (0 until n).sumOf { abs(a[it] - a.average()) } / n
        if (spread > 0.0 && sad[best] / spread > MAX_RESIDUAL) return null
        var offset = (best - reach).toDouble()
        if (best in 1 until sad.size - 1) {
            val l = sad[best - 1]
            val c = sad[best]
            val r = sad[best + 1]
            val denom = l - 2 * c + r
            if (denom > 1e-9) offset += 0.5 * (l - r) / denom
        }
        return offset
    }
}
