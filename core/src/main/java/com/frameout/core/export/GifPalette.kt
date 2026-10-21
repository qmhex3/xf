package com.frameout.core.export

internal class GifPalette private constructor(val table: ByteArray, private val size: Int) {
    private val cache = ShortArray(BINS) { -1 }

    fun nearest(r: Int, g: Int, b: Int): Int {
        val key = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
        val hit = cache[key].toInt()
        if (hit >= 0) return hit
        val cr = (r and 0xF8) + 4
        val cg = (g and 0xF8) + 4
        val cb = (b and 0xF8) + 4
        var best = 0
        var bestDistance = Int.MAX_VALUE
        for (i in 0 until size) {
            val dr = cr - (table[i * 3].toInt() and 0xFF)
            val dg = cg - (table[i * 3 + 1].toInt() and 0xFF)
            val db = cb - (table[i * 3 + 2].toInt() and 0xFF)
            val distance = dr * dr * 3 + dg * dg * 4 + db * db * 2
            if (distance < bestDistance) {
                bestDistance = distance
                best = i
            }
        }
        cache[key] = best.toShort()
        return best
    }

    fun red(index: Int): Int = table[index * 3].toInt() and 0xFF
    fun green(index: Int): Int = table[index * 3 + 1].toInt() and 0xFF
    fun blue(index: Int): Int = table[index * 3 + 2].toInt() and 0xFF

    companion object {
        private const val BINS = 32768
        private const val MAX_SAMPLES = 65536

        fun build(argb: IntArray, include: (Int) -> Boolean, maxColors: Int): GifPalette {
            val counts = IntArray(BINS)
            var eligible = 0
            for (i in argb.indices) if (include(i)) eligible++
            val stride = (eligible / MAX_SAMPLES).coerceAtLeast(1)
            var seen = 0
            for (i in argb.indices) {
                if (!include(i)) continue
                if (seen++ % stride != 0) continue
                val p = argb[i]
                counts[(((p shr 19) and 31) shl 10) or (((p shr 11) and 31) shl 5) or ((p shr 3) and 31)]++
            }
            val bins = (0 until BINS).filter { counts[it] > 0 }.toIntArray()
            val boxes = ArrayList<IntArray>()
            if (bins.isNotEmpty()) boxes += bins
            while (boxes.size < maxColors) {
                val target = boxes.indices.filter { boxes[it].size > 1 }.maxByOrNull { population(boxes[it], counts) } ?: break
                val (a, b) = split(boxes[target], counts)
                boxes[target] = a
                boxes += b
            }
            val table = ByteArray(256 * 3)
            boxes.forEachIndexed { i, box ->
                var r = 0L; var g = 0L; var b = 0L; var n = 0L
                for (bin in box) {
                    val c = counts[bin].toLong()
                    r += c * ((bin shr 10) and 31); g += c * ((bin shr 5) and 31); b += c * (bin and 31); n += c
                }
                val d = n.coerceAtLeast(1L)
                table[i * 3] = ((r / d) * 8 + 4).coerceAtMost(255).toInt().toByte()
                table[i * 3 + 1] = ((g / d) * 8 + 4).coerceAtMost(255).toInt().toByte()
                table[i * 3 + 2] = ((b / d) * 8 + 4).coerceAtMost(255).toInt().toByte()
            }
            return GifPalette(table, boxes.size.coerceAtLeast(1))
        }

        private fun population(box: IntArray, counts: IntArray): Long = box.sumOf { counts[it].toLong() }

        private fun split(box: IntArray, counts: IntArray): Pair<IntArray, IntArray> {
            var minR = 31; var maxR = 0; var minG = 31; var maxG = 0; var minB = 31; var maxB = 0
            for (bin in box) {
                val r = (bin shr 10) and 31; val g = (bin shr 5) and 31; val b = bin and 31
                if (r < minR) minR = r; if (r > maxR) maxR = r
                if (g < minG) minG = g; if (g > maxG) maxG = g
                if (b < minB) minB = b; if (b > maxB) maxB = b
            }
            val rangeR = (maxR - minR) * 3
            val rangeG = (maxG - minG) * 4
            val rangeB = (maxB - minB) * 2
            val shift = when {
                rangeG >= rangeR && rangeG >= rangeB -> 5
                rangeR >= rangeB -> 10
                else -> 0
            }
            val sorted = box.sortedBy { (it shr shift) and 31 }
            val half = population(box, counts) / 2
            var running = 0L
            var cut = 1
            for (i in sorted.indices) {
                running += counts[sorted[i]]
                if (running >= half) {
                    cut = (i + 1).coerceIn(1, sorted.size - 1)
                    break
                }
            }
            return sorted.subList(0, cut).toIntArray() to sorted.subList(cut, sorted.size).toIntArray()
        }
    }
}
