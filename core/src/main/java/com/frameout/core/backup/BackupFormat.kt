package com.frameout.core.backup

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupFormat {
    const val EXTENSION = "frameout"
    const val MIME = "application/x-frameout"
    const val MAGIC = "frameout-project"
    const val VERSION = 1
    const val MANIFEST = "project.fxp"
    const val MAX_MANIFEST_BYTES = 32L * 1024 * 1024
    const val MAX_THUMB_BYTES = 2L * 1024 * 1024
    const val MAX_FRAMES = 500_000
    const val MAX_THUMBS_TOTAL_BYTES = 256L * 1024 * 1024
    const val MAX_ENTRIES = MAX_FRAMES + 16
    const val MAX_TEXT_CHARS = 256

    private val THUMB_PATTERN = Regex("thumbs/(\\d{1,9})\\.jpg")
    private val UNSAFE_NAME = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    fun thumbEntry(index: Int): String = "thumbs/$index.jpg"

    fun thumbIndex(entry: String): Int? = THUMB_PATTERN.matchEntire(entry)?.groupValues?.get(1)?.toIntOrNull()

    fun isBackupName(name: String?): Boolean = name?.lowercase(Locale.ROOT)?.endsWith(".$EXTENSION") == true

    fun fileName(projectName: String, nowMs: Long): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(nowMs))
        val base = projectName.replace(UNSAFE_NAME, "_").trim().take(60).ifBlank { "project" }
        return "${base}_$stamp.$EXTENSION"
    }
}
