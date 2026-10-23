package com.frameout.core.util

object NameSanitizer {
    const val MAX_NAME_CHARS = 120

    fun clean(raw: String?, fallback: String = "", max: Int = MAX_NAME_CHARS): String {
        if (raw.isNullOrEmpty()) return fallback
        val sb = StringBuilder(minOf(raw.length, max + 8))
        var lastSpace = false
        var i = 0
        val limit = minOf(raw.length, max * 4)
        while (i < limit) {
            val cp = raw.codePointAt(i)
            i += Character.charCount(cp)
            if (isBlocked(cp)) continue
            if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
                if (!lastSpace && sb.isNotEmpty()) sb.append(' ')
                lastSpace = true
                continue
            }
            lastSpace = false
            sb.appendCodePoint(cp)
            if (sb.length >= max) break
        }
        var out = sb.toString().trim()
        if (out.length > max) out = out.take(max).trimEnd()
        if (out.isNotEmpty() && Character.isHighSurrogate(out.last())) out = out.dropLast(1)
        return out.ifBlank { fallback }
    }

    private fun isBlocked(cp: Int): Boolean {
        if (Character.isISOControl(cp)) return true
        return when (Character.getType(cp).toByte()) {
            Character.FORMAT, Character.PRIVATE_USE, Character.SURROGATE, Character.UNASSIGNED, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR -> true
            else -> false
        }
    }
}
