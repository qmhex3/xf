package com.frameout.core.quality

import com.frameout.core.model.ProjectStatus

object ScanTiming {
    fun mayAutoStart(status: String?): Boolean = status == ProjectStatus.Completed.name

    fun shouldHalt(status: String?, running: Boolean, manual: Boolean): Boolean = running && !manual && !mayAutoStart(status)
}
