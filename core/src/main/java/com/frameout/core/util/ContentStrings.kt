package com.frameout.core.util

import com.frameout.core.R

object ContentStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun title() = s(R.string.ctag_title)
    fun pickHint() = s(R.string.ctag_pick_hint)
    fun selected(n: Int) = s(R.string.ctag_selected_n, n)
    fun clear() = s(R.string.ctag_clear)
    fun matchLabel() = s(R.string.ctag_match_label)
    fun matchAny() = s(R.string.ctag_match_any)
    fun matchAll() = s(R.string.ctag_match_all)
    fun strictLabel() = s(R.string.ctag_strict_label)
    fun strictName(level: Int) = when (level) {
        0 -> s(R.string.ctag_strict_loose)
        2 -> s(R.string.ctag_strict_strict)
        else -> s(R.string.ctag_strict_balanced)
    }
    fun strictHelp(level: Int) = when (level) {
        0 -> s(R.string.ctag_strict_loose_help)
        2 -> s(R.string.ctag_strict_strict_help)
        else -> s(R.string.ctag_strict_balanced_help)
    }
    fun groupAll() = s(R.string.ctag_group_all)
    fun groupNone() = s(R.string.ctag_group_none)
    fun slow() = s(R.string.ctag_slow)
    fun noneSelected() = s(R.string.ctag_none_selected)
    fun matchHelp(all: Boolean) = s(if (all) R.string.ctag_match_all_help else R.string.ctag_match_any_help)
}
