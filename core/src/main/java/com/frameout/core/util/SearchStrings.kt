package com.frameout.core.util

import com.frameout.core.R

object SearchStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun action() = s(R.string.search_image)
    fun actionShort() = s(R.string.search_image_short)
    fun title() = s(R.string.search_image_title)
    fun sheetHint() = s(R.string.search_sheet_hint)
    fun recommended() = s(R.string.search_recommended)
    fun savedHint() = s(R.string.search_saved_hint)
    fun failed() = s(R.string.search_failed)
    fun opening() = s(R.string.search_opening)
    fun webRetry() = s(R.string.search_web_retry)
    fun webSite() = s(R.string.search_web_site)
    fun webFallback() = s(R.string.search_web_fallback)
}
