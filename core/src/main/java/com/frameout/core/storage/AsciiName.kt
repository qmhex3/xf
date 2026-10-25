package com.frameout.core.storage

import java.text.Normalizer

object AsciiName {
    private val allowedPunctuation = setOf(' ', '.', '_', '-', '(', ')')

    fun orNull(raw: String): String? {
        val folded = Normalizer.normalize(raw, Normalizer.Form.NFD)
        val sb = StringBuilder(folded.length)
        for (ch in folded) {
            when {
                ch.code < 128 && (ch.isLetterOrDigit() || ch in allowedPunctuation) -> sb.append(ch)
                ch.isWhitespace() && sb.isNotEmpty() -> sb.append(' ')
            }
        }
        val cleaned = sb.toString().replace(Regex("\\s+"), " ").replace(Regex("_{2,}"), "_").trim().trim('.', '_', '-').trim()
        return cleaned.takeIf { name -> name.any { it.isLetterOrDigit() } }
    }

    fun folder(raw: String, projectId: Long): String =
        if (raw.isBlank()) "" else orNull(raw) ?: "project_$projectId"

    fun fileName(name: String, fallbackStem: String): String {
        val stem = name.substringBeforeLast('.', name)
        val ext = orNull(name.substringAfterLast('.', ""))?.filter { it.isLetterOrDigit() }.orEmpty()
        val cleaned = orNull(stem) ?: fallbackStem
        return if (ext.isEmpty()) cleaned else "$cleaned.$ext"
    }
}
