package com.frameout.engine.decode

import android.graphics.Bitmap
import android.graphics.Gainmap
import android.os.Build
import androidx.annotation.RequiresApi

internal class GainmapBuilder(private val width: Int, private val height: Int) {
    private val planes = Array(3) { ShortArray(width * height) }

    fun add(index: Int, codes: IntArray) {
        planes[0][index] = codes[0].toShort()
        planes[1][index] = codes[1].toShort()
        planes[2][index] = codes[2].toShort()
    }

    fun attach(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT < 34) return
        val grids = Array(3) { gridFor(planes[it]) }
        val map = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        map.setPixels(contents(grids), 0, width, 0, 0, width, height)
        Api34.attach(bitmap, map, grids)
    }

    private fun gridFor(plane: ShortArray): GainCodec.Grid {
        var lo = 65535
        var hi = 0
        for (value in plane) {
            val v = value.toInt() and 0xFFFF
            if (v < lo) lo = v
            if (v > hi) hi = v
        }
        if (plane.isEmpty() || hi < lo) {
            lo = 0
            hi = 0
        }
        return GainCodec.Grid(lo, hi)
    }

    private fun contents(grids: Array<GainCodec.Grid>): IntArray = IntArray(width * height) { i ->
        val r = grids[0].byteOf(planes[0][i].toInt() and 0xFFFF)
        val g = grids[1].byteOf(planes[1][i].toInt() and 0xFFFF)
        val b = grids[2].byteOf(planes[2][i].toInt() and 0xFFFF)
        -0x1000000 or (r shl 16) or (g shl 8) or b
    }

    @RequiresApi(34)
    private object Api34 {
        fun attach(bitmap: Bitmap, map: Bitmap, grids: Array<GainCodec.Grid>) {
            val gainmap = Gainmap(map)
            gainmap.setRatioMin(grids[0].ratioMin, grids[1].ratioMin, grids[2].ratioMin)
            gainmap.setRatioMax(grids[0].ratioMax, grids[1].ratioMax, grids[2].ratioMax)
            gainmap.setGamma(1f, 1f, 1f)
            gainmap.setEpsilonSdr(GainCodec.EPS, GainCodec.EPS, GainCodec.EPS)
            gainmap.setEpsilonHdr(GainCodec.EPS, GainCodec.EPS, GainCodec.EPS)
            gainmap.setMinDisplayRatioForHdrTransition(1f)
            gainmap.setDisplayRatioForFullHdr(maxOf(grids[0].ratioMax, grids[1].ratioMax, grids[2].ratioMax, 1.0001f))
            bitmap.setGainmap(gainmap)
        }
    }
}
