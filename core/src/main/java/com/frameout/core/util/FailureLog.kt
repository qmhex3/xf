package com.frameout.core.util

import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap

object FailureLog {
    private val last = ConcurrentHashMap<String, String>()

    fun once(tag: String, error: Throwable) {
        if (error is CancellationException) return
        val line = "${error.javaClass.simpleName}: ${error.message}"
        if (last.put(tag, line) == line) return
        DiagLog.append(tag, line)
    }
}
