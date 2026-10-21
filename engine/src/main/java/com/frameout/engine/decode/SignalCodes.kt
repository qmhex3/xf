package com.frameout.engine.decode

import android.media.MediaFormat
import com.frameout.core.hdr.HdrSignal

internal object SignalCodes {
    fun primaries(params: ColorParams): Int = when (params.gamutClass) {
        0 -> HdrSignal.PRIMARIES_BT709
        2 -> HdrSignal.PRIMARIES_DISPLAY_P3
        3 -> DCI_P3
        else -> HdrSignal.PRIMARIES_BT2020
    }

    fun transfer(params: ColorParams): Int =
        if (params.transfer == MediaFormat.COLOR_TRANSFER_HLG) HdrSignal.TRANSFER_HLG else HdrSignal.TRANSFER_PQ

    private const val DCI_P3 = 11
}
