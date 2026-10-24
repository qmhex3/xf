package com.frameout.core.util

import com.frameout.core.R

object UpdateStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun badgeNew() = s(R.string.update_badge_new)
    fun title() = s(R.string.update_title)
    fun meta(version: String, size: String) = s(R.string.update_meta, version, size)
    fun feature1Title() = s(R.string.update_f1_title)
    fun feature1Body() = s(R.string.update_f1_body)
    fun feature2Title() = s(R.string.update_f2_title)
    fun feature2Body() = s(R.string.update_f2_body)
    fun feature3Title() = s(R.string.update_f3_title)
    fun feature3Body() = s(R.string.update_f3_body)
    fun fixesTitle(count: Int) = s(R.string.update_fixes_title, count)
    fun fixesBody() = s(R.string.update_fixes_body)
    fun later() = s(R.string.update_later)
    fun now() = s(R.string.update_now)
}
