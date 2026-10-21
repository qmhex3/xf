package com.frameout.core.storage

import java.util.Locale

object FrameNaming {
    const val DEFAULT = "FrameOut_{id}_{index}"
    private const val MAX_LENGTH = 120
    private const val MAX_TEMPLATE = 80
    private val forbidden = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    private val token = Regex("\\{(project|id|index|n|time)\\}")

    fun cleanTemplate(raw: String): String = raw.filter { it != '\n' && it != '\r' }.take(MAX_TEMPLATE)

    fun render(template: String, project: String, projectId: Long, frameIndex: Int, ptsUs: Long, sequence: Int, total: Int, ext: String): String {
        val source = template.ifBlank { DEFAULT }
        val width = maxOf(total.toString().length, 4)
        val base = token.replace(source) { match ->
            when (match.groupValues[1]) {
                "project" -> project
                "id" -> projectId.toString()
                "index" -> frameIndex.toString()
                "n" -> sequence.toString().padStart(width, '0')
                else -> time(ptsUs)
            }
        }
        val cleaned = base.replace(forbidden, "_").trim().trim('.').take(MAX_LENGTH).trim()
        val safe = cleaned.ifBlank { "FrameOut_${projectId}_$frameIndex" }
        return "$safe.$ext"
    }

    fun time(ptsUs: Long): String {
        val totalMs = (ptsUs / 1000L).coerceAtLeast(0L)
        return String.format(Locale.US, "%02dh%02dm%02ds%03d", totalMs / 3_600_000L, totalMs / 60_000L % 60L, totalMs / 1000L % 60L, totalMs % 1000L)
    }

    fun unique(name: String, used: MutableSet<String>): String {
        if (used.add(name.lowercase(Locale.ROOT))) return name
        val stem = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var counter = 2
        while (true) {
            val candidate = if (ext.isEmpty()) "${stem}_$counter" else "${stem}_$counter.$ext"
            if (used.add(candidate.lowercase(Locale.ROOT))) return candidate
            counter++
        }
    }
}
