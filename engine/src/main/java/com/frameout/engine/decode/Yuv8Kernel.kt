package com.frameout.engine.decode

internal class Yuv8Kernel {
    private val cols = Yuv8Columns()

    fun convert(src: Yuv8Planes, params: ColorParams, step: Int, width: Int, height: Int, pixels: IntArray, orient: Orient) {
        val taps = DecodeBudget.taps(step)
        cols.build(src, step, taps, width)
        val job = Yuv8Job(src, params, cols, step, taps, width, pixels, orient)
        YuvPool.run(YuvPool.bands(width, height), height) { first, last -> job.render(first, last) }
    }

    fun release() = cols.release()
}
