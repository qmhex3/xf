package com.frameout.core.util

import com.frameout.codecpack.RuntimeIntegrityException

object RuntimeGate {
    @Volatile var probe: () -> Boolean = { true }

    suspend fun pass() {
        if (probe()) return
        DiagLog.append("integrity", "runtime check failed; extraction blocked")
        throw RuntimeIntegrityException()
    }
}
