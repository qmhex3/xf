package com.frameout.core.quality

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

object ScanThrottle {
    val scrolling = MutableStateFlow(false)

    suspend fun awaitIdle() {
        if (scrolling.value) scrolling.first { !it }
    }
}
