package com.frameout.core.quality

import java.util.Random
import kotlin.math.roundToInt

internal object SyntheticFrames {
    const val W = 160
    const val H = 90

    fun scene(seed: Int, w: Int = W, h: Int = H): IntArray {
        val rnd = Random(seed.toLong())
        val out = IntArray(w * h) { i -> 90 + (i % w) * 40 / w }
        repeat(14) {
            val rw = 6 + rnd.nextInt(w / 4)
            val rh = 6 + rnd.nextInt(h / 4)
            val x0 = rnd.nextInt(w - rw)
            val y0 = rnd.nextInt(h - rh)
            val v = 20 + rnd.nextInt(215)
            for (y in y0 until y0 + rh) for (x in x0 until x0 + rw) out[y * w + x] = v
        }
        repeat(6) {
            val y = rnd.nextInt(h)
            val v = 15 + rnd.nextInt(225)
            for (x in 0 until w) out[y * w + x] = v
        }
        return out
    }

    fun boxBlur(src: IntArray, w: Int, h: Int, radius: Int): IntArray {
        if (radius <= 0) return src.copyOf()
        val tmp = IntArray(src.size)
        val out = IntArray(src.size)
        for (y in 0 until h) for (x in 0 until w) {
            var s = 0
            var n = 0
            for (k in -radius..radius) { val xx = x + k; if (xx in 0 until w) { s += src[y * w + xx]; n++ } }
            tmp[y * w + x] = s / n
        }
        for (y in 0 until h) for (x in 0 until w) {
            var s = 0
            var n = 0
            for (k in -radius..radius) { val yy = y + k; if (yy in 0 until h) { s += tmp[yy * w + x]; n++ } }
            out[y * w + x] = s / n
        }
        return out
    }

    fun noisy(src: IntArray, sigma: Double, seed: Int): IntArray {
        val rnd = Random(seed.toLong())
        return IntArray(src.size) { (src[it] + rnd.nextGaussian() * sigma).roundToInt().coerceIn(0, 255) }
    }

    fun scaled(src: IntArray, factor: Double): IntArray = IntArray(src.size) { (src[it] * factor).roundToInt().coerceIn(0, 255) }

    fun shifted(src: IntArray, delta: Int): IntArray = IntArray(src.size) { (src[it] + delta).coerceIn(0, 255) }

    fun withBox(src: IntArray, w: Int, x0: Int, y0: Int, bw: Int, bh: Int, value: Int): IntArray {
        val out = src.copyOf()
        for (y in y0 until y0 + bh) for (x in x0 until x0 + bw) out[y * w + x] = value
        return out
    }

    fun panned(src: IntArray, w: Int, h: Int, dx: Int): IntArray = IntArray(src.size) { i ->
        val x = (i % w - dx).coerceIn(0, w - 1)
        src[(i / w) * w + x]
    }

    fun verticalBars(w: Int, h: Int, bar: Int): IntArray = IntArray(w * h) { i -> if ((i % w) / bar % 2 == 0) 40 else 215 }

    fun checker(w: Int, h: Int, cell: Int): IntArray = IntArray(w * h) { i -> if (((i % w) / cell + (i / w) / cell) % 2 == 0) 40 else 215 }
}
