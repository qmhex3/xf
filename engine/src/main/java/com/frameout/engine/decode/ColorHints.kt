package com.frameout.engine.decode

import android.media.MediaFormat

internal object ColorHints {
    const val KEY_PRIMARIES = "frameout-colr-primaries"
    const val KEY_MATRIX = "frameout-colr-matrix"

    const val PRIMARIES_BT709 = 1
    const val PRIMARIES_BT2020 = 9
    const val PRIMARIES_DCI_P3 = 11
    const val PRIMARIES_DISPLAY_P3 = 12

    fun apply(format: MediaFormat, colr: Mp4Orientation.Colr) {
        if (colr.rangeKnown && !format.containsKey(MediaFormat.KEY_COLOR_RANGE)) {
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, if (colr.full) MediaFormat.COLOR_RANGE_FULL else MediaFormat.COLOR_RANGE_LIMITED)
        }
        if (!format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) {
            standard(colr)?.let { format.setInteger(MediaFormat.KEY_COLOR_STANDARD, it) }
        }
        if (!format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
            transfer(colr.transfer)?.let { format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, it) }
        }
        if (!format.containsKey(KEY_PRIMARIES)) format.setInteger(KEY_PRIMARIES, colr.primaries)
        if (!format.containsKey(KEY_MATRIX)) format.setInteger(KEY_MATRIX, colr.matrix)
    }

    fun applyVui(format: MediaFormat, vui: VuiColor.Signal) {
        format.setInteger(MediaFormat.KEY_COLOR_RANGE, if (vui.fullRange) MediaFormat.COLOR_RANGE_FULL else MediaFormat.COLOR_RANGE_LIMITED)
        if (!vui.hasDescription || (vui.primaries == 2 && vui.transfer == 2 && vui.matrix == 2)) return
        val colr = Mp4Orientation.Colr(vui.primaries, vui.transfer, vui.matrix, vui.fullRange)
        standard(colr)?.let { format.setInteger(MediaFormat.KEY_COLOR_STANDARD, it) }
        transfer(colr.transfer)?.let { format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, it) }
        format.setInteger(KEY_PRIMARIES, vui.primaries)
        format.setInteger(KEY_MATRIX, vui.matrix)
    }

    private fun standard(colr: Mp4Orientation.Colr): Int? = when (colr.matrix) {
        1 -> MediaFormat.COLOR_STANDARD_BT709
        5 -> MediaFormat.COLOR_STANDARD_BT601_PAL
        6 -> MediaFormat.COLOR_STANDARD_BT601_NTSC
        9, 10, 14 -> MediaFormat.COLOR_STANDARD_BT2020
        else -> if (colr.primaries == PRIMARIES_BT2020) MediaFormat.COLOR_STANDARD_BT2020 else null
    }

    private fun transfer(code: Int): Int? = when (code) {
        16 -> MediaFormat.COLOR_TRANSFER_ST2084
        18 -> MediaFormat.COLOR_TRANSFER_HLG
        1, 6, 11, 13, 14, 15 -> MediaFormat.COLOR_TRANSFER_SDR_VIDEO
        8 -> MediaFormat.COLOR_TRANSFER_LINEAR
        else -> null
    }
}
