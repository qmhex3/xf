package com.frameout.app.ads

import com.frameout.core.ads.AdPlacement

object AdsGateway {
    @Volatile
    var hub: AdsHub? = null

    fun action() {
        hub?.noteAction()
    }

    fun exportDone() {
        hub?.let {
            it.noteAction()
            it.onNaturalBreak(AdPlacement.ExportDone)
        }
    }

    fun screenBreak() {
        hub?.onNaturalBreak(AdPlacement.ScreenBreak)
    }
}
