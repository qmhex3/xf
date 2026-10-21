package com.frameout.app.security

import android.content.Context
import com.frameout.core.util.DiagLog
import com.frameout.core.util.RuntimeGate

object Sentinel {
    private const val RECHECK_MS = 45_000L
    @Volatile private var verdict = Verdict.Clean
    @Volatile private var checkedAt = 0L

    fun install(context: Context) {
        val app = context.applicationContext
        RuntimeGate.probe = { verdict(app) == Verdict.Clean }
    }

    fun verdict(context: Context): Verdict {
        val now = System.currentTimeMillis()
        if (checkedAt != 0L && now - checkedAt < RECHECK_MS) return verdict
        val fresh = IntegrityGuard.inspect(context)
        if (fresh != Verdict.Clean && fresh != verdict) DiagLog.append("integrity", "verdict=$fresh")
        verdict = fresh
        checkedAt = now
        return fresh
    }
}
