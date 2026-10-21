package com.frameout.engine.decode

internal class Orient private constructor(
    val outWidth: Int,
    val outHeight: Int,
    private val base: Int,
    private val colStep: Int,
    private val rowStep: Int
) {
    fun index(col: Int, row: Int): Int = base + col * colStep + row * rowStep

    companion object {
        fun of(rotation: Int, flipH: Boolean, w: Int, h: Int): Orient = when (FrameOrientation.normalize(rotation)) {
            90 -> if (flipH) Orient(h, w, (w - 1) * h + h - 1, -h, -1) else Orient(h, w, h - 1, h, -1)
            180 -> if (flipH) Orient(w, h, (h - 1) * w, 1, -w) else Orient(w, h, w * h - 1, -1, -w)
            270 -> if (flipH) Orient(h, w, 0, h, 1) else Orient(h, w, (w - 1) * h, -h, 1)
            else -> if (flipH) Orient(w, h, w - 1, -1, w) else Orient(w, h, 0, 1, w)
        }
    }
}
