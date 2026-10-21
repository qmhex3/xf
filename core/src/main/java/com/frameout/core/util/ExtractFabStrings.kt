package com.frameout.core.util

import com.frameout.core.R

object ExtractFabStrings {
    fun percent(value: Int): String = StringSource.get(R.string.progress_percent, value)
}
