package com.frameout.core.util

import com.frameout.core.R

object FrameA11y {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun select() = s(R.string.select_item)
    fun frame(index: Int, time: String) = s(R.string.frame_description, index, time)
    fun timeline() = s(R.string.timeline)
    fun timelineState(position: String, start: String, end: String) = s(R.string.timeline_state, position, start, end)
    fun startEarlier() = s(R.string.start_earlier)
    fun startLater() = s(R.string.start_later)
    fun endEarlier() = s(R.string.end_earlier)
    fun endLater() = s(R.string.end_later)
    fun cropArea() = s(R.string.crop_area)
}
