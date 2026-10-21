package com.frameout.core.storage

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ExifText {
    fun dateTime(millis: Long, zone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply { timeZone = zone }.format(Date(millis))

    fun subSecond(millis: Long): String = (((millis % 1000L) + 1000L) % 1000L).toString().padStart(3, '0')

    fun position(ptsUs: Long): String {
        val totalMs = (ptsUs / 1000L).coerceAtLeast(0L)
        val hours = totalMs / 3_600_000L
        val minutes = totalMs / 60_000L % 60L
        val seconds = totalMs / 1000L % 60L
        val prefix = if (hours > 0) "%d:%02d:%02d".format(Locale.US, hours, minutes, seconds) else "%02d:%02d".format(Locale.US, minutes, seconds)
        return "$prefix.%03d".format(Locale.US, totalMs % 1000L)
    }

    fun ascii(text: String): String = buildString(text.length) {
        text.forEach { c ->
            append(
                when {
                    c == '\u00B7' -> '-'
                    c.code in 0x20..0x7E -> c
                    else -> '?'
                }
            )
        }
    }

    fun description(videoName: String, frameIndex: Int, ptsUs: Long): String =
        "$videoName · #$frameIndex · ${position(ptsUs)}"
}
