package com.frameout.core.util

import com.frameout.core.R

object ScrubStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun fastScroll() = s(R.string.fast_scroll)
    fun scrubPosition(index: Int, total: Int) = s(R.string.scrub_position, index, total)
    fun selectedOfTotal(selected: Int, total: Int) = s(R.string.selected_of_total, selected, total)
}
