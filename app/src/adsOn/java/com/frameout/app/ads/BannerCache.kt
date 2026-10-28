package com.frameout.app.ads

import android.app.Activity
import android.content.Context
import android.content.MutableContextWrapper
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * One banner for the whole app. Screens only borrow it, so leaving and re-entering the projects screen shows the
 * already loaded ad at once instead of creating a new AdView and waiting for a new request.
 */
internal object BannerCache {
    private var wrapper: MutableContextWrapper? = null
    private var view: AdView? = null
    private var unitId = ""
    private var widthDp = 0
    private var active = true

    var loaded = false
        private set
    private var inFlight = false

    var sink: (AdLoad) -> Unit = {}

    fun heightDp(context: Context, widthDp: Int): Int =
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp).height

    fun obtain(context: Context, unit: String, width: Int, result: (AdLoad) -> Unit): AdView {
        sink = result
        view?.let {
            if (unit == unitId && width == widthDp) {
                if (!loaded && !inFlight) request(it)
                return it
            }
        }
        release()
        val app = context.applicationContext
        val base = MutableContextWrapper(app)
        wrapper = base
        unitId = unit
        widthDp = width
        loaded = false
        val created = AdView(base).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(app, width))
            adUnitId = unit
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    loaded = true
                    inFlight = false
                    sink(AdLoad(true))
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    inFlight = false
                    sink(AdLoad(false, error.code))
                }
            }
        }
        view = created
        if (!active) created.pause()
        request(created)
        return created
    }

    private fun request(target: AdView) {
        inFlight = true
        target.loadAd(AdRequest.Builder().build())
    }

    fun setActive(value: Boolean) {
        active = value
        if (value) view?.resume() else view?.pause()
    }

    fun bind(activity: Activity?) {
        if (activity != null) {
            wrapper?.setBaseContext(activity)
            return
        }
        detach()
        wrapper?.let { it.setBaseContext(it.applicationContext) }
    }

    fun detach() {
        (view?.parent as? ViewGroup)?.removeView(view)
    }

    private fun release() {
        view?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        view = null
        wrapper = null
        loaded = false
        inFlight = false
    }
}
