package com.frameout.app.ads

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

object AdsCountry {
    fun iso(context: Context): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val sim = tm?.simCountryIso.orEmpty()
        val net = tm?.networkCountryIso.orEmpty()
        return sim.ifBlank { net }.ifBlank { Locale.getDefault().country }
    }
}
