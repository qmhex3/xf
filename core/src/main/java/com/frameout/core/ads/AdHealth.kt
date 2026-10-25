package com.frameout.core.ads

class AdHealth(private val store: AdStore) {
    var state: AdState = AdState.Unknown
        private set

    fun onLoaded(nowWall: Long) {
        store.put(SUSPECT, 0)
        store.put(LAST_OK, nowWall)
        state = AdState.Healthy
    }

    fun onFailed(code: Int, online: Boolean, sinkDns: Boolean, nowWall: Long): AdState {
        val suspicious = online && (sinkDns || code == NETWORK_ERROR || code == INTERNAL_ERROR)
        if (code == NO_FILL && !sinkDns) {
            store.put(SUSPECT, 0)
            state = AdState.NoFill
            return state
        }
        if (!suspicious) return state
        val count = store.int(SUSPECT) + 1
        store.put(SUSPECT, count)
        val recentlyOk = nowWall - store.long(LAST_OK) < WEEK_MS && store.long(LAST_OK) > 0
        state = if (sinkDns || (count >= THRESHOLD && !recentlyOk)) AdState.Blocked else state
        return state
    }

    fun shouldNotify(nowWall: Long): Boolean {
        if (state != AdState.Blocked) return false
        val last = store.long(LAST_NOTICE)
        return last == 0L || nowWall - last >= NOTICE_GAP_MS || nowWall < last
    }

    fun noticeShown(nowWall: Long) {
        store.put(LAST_NOTICE, nowWall)
    }

    companion object {
        const val INTERNAL_ERROR = 0
        const val NETWORK_ERROR = 2
        const val NO_FILL = 3
        private const val THRESHOLD = 4
        private const val WEEK_MS = 7 * 86_400_000L
        private const val NOTICE_GAP_MS = 7 * 86_400_000L
        private const val SUSPECT = "ad_suspect"
        private const val LAST_OK = "ad_last_ok"
        private const val LAST_NOTICE = "ad_last_notice"
    }
}
