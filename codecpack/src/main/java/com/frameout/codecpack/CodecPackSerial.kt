package com.frameout.codecpack

internal object CodecPackSerial {
    private val LINE = Regex("(?m)^Serial: (\\d{1,18})\\s*$")

    fun parse(payload: String): Long = LINE.find(payload)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

    fun rejection(candidate: Long, installed: Long, minimum: Long, revoked: Set<Long> = emptySet()): String? = when {
        candidate in revoked -> "serial revoked"
        candidate < minimum -> "serial below minimum"
        candidate < installed -> "serial older than installed pack"
        else -> null
    }
}
