package com.frameout.engine.decode

import android.media.MediaFormat

internal object ColorVariants {
    const val AUTO = 0

    val CANDIDATES: IntArray = intArrayOf(1, 2, 3, 9, 4, 5, 6, 7, 8)

    fun apply(id: Int, p: ColorParams): ColorParams = when (id) {
        1 -> sdr(p, p.bt2020, p.bt601, p.matrix, p.limited)
        2 -> sdr(p, false, false, 0, p.limited)
        3 -> sdr(p, false, true, 0, p.limited)
        4 -> sdr(p, true, false, 0, p.limited)
        5 -> sdr(p, p.bt2020, p.bt601, p.matrix, !p.limited)
        6 -> sdr(p, false, false, 0, !p.limited)
        7 -> sdr(p, false, true, 0, !p.limited)
        8 -> sdr(p, true, false, 0, !p.limited)
        9 -> ColorParams(!p.limited, p.bt2020, p.transfer, p.peakNits, p.bt601, p.primaries, true, p.matrix)
        else -> p
    }

    fun describe(id: Int): String = when (id) {
        AUTO -> "auto (metadata as read)"
        1 -> "SDR, metadata matrix, no gamut conversion"
        2 -> "SDR BT.709"
        3 -> "SDR BT.601"
        4 -> "SDR BT.2020 matrix, no gamut conversion"
        9 -> "metadata with inverted range"
        in 5..8 -> "${describe(id - 4)}, inverted range"
        else -> "variant $id"
    }

    private fun sdr(p: ColorParams, bt2020: Boolean, bt601: Boolean, matrix: Int, limited: Boolean): ColorParams =
        ColorParams(limited, bt2020, MediaFormat.COLOR_TRANSFER_SDR_VIDEO, p.peakNits, bt601, 0, true, matrix)
}
