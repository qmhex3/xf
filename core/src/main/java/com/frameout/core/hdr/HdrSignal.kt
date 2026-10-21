package com.frameout.core.hdr

class HdrSignal(val width: Int, val height: Int, val rgb: ShortArray, val primaries: Int, val transfer: Int) {
    init {
        require(width > 0 && height > 0 && rgb.size == width * height * 3)
    }

    companion object {
        const val PRIMARIES_BT709 = 1
        const val PRIMARIES_BT2020 = 9
        const val PRIMARIES_DCI_P3 = 11
        const val PRIMARIES_DISPLAY_P3 = 12
        const val TRANSFER_PQ = 16
        const val TRANSFER_HLG = 18

        fun supports(primaries: Int, transfer: Int): Boolean =
            primaries in setOf(PRIMARIES_BT709, PRIMARIES_BT2020, PRIMARIES_DCI_P3, PRIMARIES_DISPLAY_P3) &&
                transfer in setOf(TRANSFER_PQ, TRANSFER_HLG)
    }
}
