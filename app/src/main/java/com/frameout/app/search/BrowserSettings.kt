package com.frameout.app.search

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView

@SuppressLint("SetJavaScriptEnabled")
internal fun applyBrowserSettings(web: WebView) {
    with(web.settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        userAgentString = userAgentString.replace("; wv", "").replace(" Version/4.0", "")
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)
        useWideViewPort = true
        loadWithOverviewMode = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
    }
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(web, true)
    }
}
