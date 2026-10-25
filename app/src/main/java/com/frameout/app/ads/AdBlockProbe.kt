package com.frameout.app.ads

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.InetAddress

object AdBlockProbe {
    private val adHosts = listOf("googleads.g.doubleclick.net", "pagead2.googlesyndication.com")
    private const val CONTROL_HOST = "www.google.com"

    fun online(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun adDnsSinkholed(): Boolean {
        if (resolve(CONTROL_HOST) == null) return false
        return adHosts.all { host -> resolve(host).let { it == null || isSink(it) } }
    }

    private fun resolve(host: String): InetAddress? = runCatching { InetAddress.getByName(host) }.getOrNull()

    private fun isSink(address: InetAddress): Boolean =
        address.isAnyLocalAddress || address.isLoopbackAddress
}
