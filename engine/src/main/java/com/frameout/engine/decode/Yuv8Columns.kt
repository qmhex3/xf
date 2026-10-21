package com.frameout.engine.decode

internal class Yuv8Columns {
    var y = IntArray(0)
        private set
    var u0 = IntArray(0)
        private set
    var u1 = IntArray(0)
        private set
    var v0 = IntArray(0)
        private set
    var v1 = IntArray(0)
        private set
    var fx = FloatArray(0)
        private set
    var cx0 = IntArray(0)
        private set
    var cx1 = IntArray(0)
        private set

    fun build(src: Yuv8Planes, step: Int, taps: Int, width: Int) {
        ensure(width * taps)
        val sampler = ChromaSampler(src.chromaWidth, src.chromaHeight)
        val crop = src.crop
        for (col in 0 until width) {
            for (tx in 0 until taps) {
                val sc = DecodeBudget.sourceIndex(col, step, tx, taps, crop.width()) + crop.left
                sampler.col(sc)
                val i = col * taps + tx
                y[i] = sc * src.yPixelStride
                u0[i] = sampler.x0 * src.uPixelStride
                u1[i] = sampler.x1 * src.uPixelStride
                v0[i] = sampler.x0 * src.vPixelStride
                v1[i] = sampler.x1 * src.vPixelStride
                fx[i] = sampler.fx
                cx0[i] = sampler.x0
                cx1[i] = sampler.x1
            }
        }
    }

    private fun ensure(size: Int) {
        if (y.size >= size) return
        y = IntArray(size)
        u0 = IntArray(size)
        u1 = IntArray(size)
        v0 = IntArray(size)
        v1 = IntArray(size)
        fx = FloatArray(size)
        cx0 = IntArray(size)
        cx1 = IntArray(size)
    }

    fun release() {
        y = IntArray(0)
        u0 = IntArray(0)
        u1 = IntArray(0)
        v0 = IntArray(0)
        v1 = IntArray(0)
        fx = FloatArray(0)
        cx0 = IntArray(0)
        cx1 = IntArray(0)
    }
}
