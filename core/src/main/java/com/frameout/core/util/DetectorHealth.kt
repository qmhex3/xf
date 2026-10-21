package com.frameout.core.util

import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object DetectorHealth {
    private class Stat {
        val failures = AtomicInteger()
        @Volatile var lastError: String = ""
    }

    private val stats = ConcurrentHashMap<String, Stat>()

    fun failed(name: String, error: Throwable) {
        if (error is CancellationException) return
        val stat = stats.getOrPut(name) { Stat() }
        val line = "${error.javaClass.simpleName}: ${error.message}"
        val count = stat.failures.incrementAndGet()
        val changed = stat.lastError != line
        stat.lastError = line
        if (changed || isPowerOfTwo(count)) DiagLog.append(name, "failure #$count $line")
    }

    fun failures(name: String): Int = stats[name]?.failures?.get() ?: 0

    fun lastError(name: String): String? = stats[name]?.lastError?.takeIf { it.isNotEmpty() }

    fun snapshot(): Map<String, Int> = stats.mapValues { it.value.failures.get() }

    fun reset() = stats.clear()

    internal fun isPowerOfTwo(n: Int): Boolean = n > 0 && (n and (n - 1)) == 0
}
