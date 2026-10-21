package com.frameout.engine.decode

internal class Yuv8RowState(private val src: Yuv8Planes, private val step: Int, private val taps: Int) {
    private val sampler = ChromaSampler(src.chromaWidth, src.chromaHeight)
    val y = IntArray(taps)
    val u0 = IntArray(taps)
    val u1 = IntArray(taps)
    val v0 = IntArray(taps)
    val v1 = IntArray(taps)
    val fy = FloatArray(taps)

    fun load(row: Int) {
        val crop = src.crop
        for (ty in 0 until taps) {
            val sr = DecodeBudget.sourceIndex(row, step, ty, taps, crop.height()) + crop.top
            sampler.row(sr)
            y[ty] = sr * src.yRowStride
            u0[ty] = src.uBase + sampler.y0 * src.uRowStride
            u1[ty] = src.uBase + sampler.y1 * src.uRowStride
            v0[ty] = src.vBase + sampler.y0 * src.vRowStride
            v1[ty] = src.vBase + sampler.y1 * src.vRowStride
            fy[ty] = sampler.fy
        }
    }
}
