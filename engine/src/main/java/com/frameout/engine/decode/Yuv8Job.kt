package com.frameout.engine.decode

internal class Yuv8Job(
    private val src: Yuv8Planes,
    private val params: ColorParams,
    private val cols: Yuv8Columns,
    private val step: Int,
    private val taps: Int,
    private val width: Int,
    private val pixels: IntArray,
    private val orient: Orient
) {
    private val yTab = params.lumaTable()
    private val cTab = params.chromaTable()
    private val rv = params.rv
    private val gu = params.gu
    private val gv = params.gv
    private val bu = params.bu
    private val special = params.special
    private val gamut = params.gamutConvert
    private val matrix = params.gamutMatrix
    private val inv = 1f / (taps * taps)

    fun render(first: Int, last: Int) {
        if (first >= last) return
        val mapper = if (params.hdr) HdrToneMapper(params) else null
        if (taps == 1) {
            renderSingle(first, last, mapper)
            return
        }
        val rows = Yuv8RowState(src, step, taps)
        val tmp = FloatArray(3)
        val rgb = FloatArray(3)
        for (row in first until last) {
            rows.load(row)
            renderRow(row, rows, tmp, rgb, mapper)
        }
    }

    private fun renderSingle(first: Int, last: Int, mapper: HdrToneMapper?) {
        val rows = Yuv8RowState(src, 1, 1)
        val chromaWidth = src.chromaWidth
        val uRow = FloatArray(chromaWidth)
        val vRow = FloatArray(chromaWidth)
        val tmp = FloatArray(3)
        val rgb = FloatArray(3)
        for (row in first until last) {
            rows.load(row)
            blendChroma(rows, chromaWidth, uRow, vRow)
            val yBase = rows.y[0]
            for (col in 0 until width) {
                val a = cols.cx0[col]
                val b = cols.cx1[col]
                val fx = cols.fx[col]
                val uu = uRow[a] + (uRow[b] - uRow[a]) * fx
                val vv = vRow[a] + (vRow[b] - vRow[a]) * fx
                val yy = yTab[src.y[yBase + cols.y[col]].toInt() and 255]
                if (special != 0) {
                    SpecialMatrix.convert(params, yy, uu, vv, rgb)
                    pixels[orient.index(col, row)] = pack(rgb[0], rgb[1], rgb[2], tmp, mapper, col, row)
                } else {
                    pixels[orient.index(col, row)] = pack(yy + rv * vv, yy - gu * uu - gv * vv, yy + bu * uu, tmp, mapper, col, row)
                }
            }
        }
    }

    private fun blendChroma(rows: Yuv8RowState, chromaWidth: Int, uRow: FloatArray, vRow: FloatArray) {
        val fy = rows.fy[0]
        val u0 = rows.u0[0]
        val u1 = rows.u1[0]
        val v0 = rows.v0[0]
        val v1 = rows.v1[0]
        val uStride = src.uPixelStride
        val vStride = src.vPixelStride
        for (cx in 0 until chromaWidth) {
            val ua = cTab[src.u[u0 + cx * uStride].toInt() and 255]
            val ub = cTab[src.u[u1 + cx * uStride].toInt() and 255]
            uRow[cx] = ua + (ub - ua) * fy
            val va = cTab[src.v[v0 + cx * vStride].toInt() and 255]
            val vb = cTab[src.v[v1 + cx * vStride].toInt() and 255]
            vRow[cx] = va + (vb - va) * fy
        }
    }

    private fun renderRow(row: Int, rows: Yuv8RowState, tmp: FloatArray, rgb: FloatArray, mapper: HdrToneMapper?) {
        for (col in 0 until width) {
            var r = 0f
            var g = 0f
            var b = 0f
            val base = col * taps
            for (ty in 0 until taps) {
                for (tx in 0 until taps) {
                    val ci = base + tx
                    val yy = yTab[src.y[rows.y[ty] + cols.y[ci]].toInt() and 255]
                    val uu = chroma(src.u, rows.u0[ty], rows.u1[ty], cols.u0[ci], cols.u1[ci], cols.fx[ci], rows.fy[ty])
                    val vv = chroma(src.v, rows.v0[ty], rows.v1[ty], cols.v0[ci], cols.v1[ci], cols.fx[ci], rows.fy[ty])
                    if (special != 0) {
                        SpecialMatrix.convert(params, yy, uu, vv, rgb)
                        r += rgb[0]; g += rgb[1]; b += rgb[2]
                    } else {
                        r += yy + rv * vv
                        g += yy - gu * uu - gv * vv
                        b += yy + bu * uu
                    }
                }
            }
            pixels[orient.index(col, row)] = pack(r * inv, g * inv, b * inv, tmp, mapper, col, row)
        }
    }

    private fun chroma(data: ByteArray, r0: Int, r1: Int, x0: Int, x1: Int, fx: Float, fy: Float): Float {
        val a = cTab[data[r0 + x0].toInt() and 255]
        val b = cTab[data[r0 + x1].toInt() and 255]
        val c = cTab[data[r1 + x0].toInt() and 255]
        val d = cTab[data[r1 + x1].toInt() and 255]
        val top = a + (b - a) * fx
        val bottom = c + (d - c) * fx
        return top + (bottom - top) * fy
    }

    private fun pack(r: Float, g: Float, b: Float, tmp: FloatArray, mapper: HdrToneMapper?, x: Int, y: Int): Int {
        if (mapper != null) return mapper.map(r, g, b, x, y)
        if (!gamut) return rgb(r, g, b)
        GamutConvert.convert(matrix, r, g, b, tmp)
        return rgb(tmp[0], tmp[1], tmp[2])
    }

    private fun rgb(r: Float, g: Float, b: Float): Int =
        (0xFF shl 24) or (clamp8(r) shl 16) or (clamp8(g) shl 8) or clamp8(b)

    private fun clamp8(value: Float): Int = (value.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
}
