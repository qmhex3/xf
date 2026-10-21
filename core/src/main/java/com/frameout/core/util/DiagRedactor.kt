package com.frameout.core.util

object DiagRedactor {
    private val uri = Regex("""\b([a-zA-Z][a-zA-Z0-9+.\-]*)://[^\s,;)\]"'<>]+""")
    private val path = Regex("""(?<![\w:/.])/(?:[^\s/:*?"<>|]+/)+[^\s/:*?"<>|]*""")
    private val media = Regex("""[^\s/\\:*?"<>|,;()\[\]]+(\.(?:mp4|m4v|mov|mkv|webm|avi|3gp|ts|gif|png|jpe?g|webp|heic|heif|apng|zip|pdf))\b""", RegexOption.IGNORE_CASE)

    fun redact(text: String): String {
        if (text.isEmpty()) return text
        val noUris = uri.replace(text) { "${it.groupValues[1]}://<redacted>" }
        val noPaths = path.replace(noUris, "<path>")
        return media.replace(noPaths) { "<file>${it.groupValues[1]}" }
    }
}
