package com.frameout.engine.extract

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

internal suspend fun awaitRunnable(isCancelled: () -> Boolean, isPaused: () -> Boolean) {
    if (isCancelled() || !coroutineContext.isActive) throw CancellationException()
    var wait = MIN_POLL_MS
    while (isPaused() && !isCancelled()) {
        delay(wait)
        wait = (wait * 2).coerceAtMost(MAX_POLL_MS)
    }
    if (isCancelled()) throw CancellationException()
}

private const val MIN_POLL_MS = 100L
private const val MAX_POLL_MS = 1_000L
