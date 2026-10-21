package com.frameout.core.backup

object BackupText {
    const val TAB = '\t'
    const val LF = '\n'

    fun escape(value: String): String {
        if (value.none { it == '\\' || it == TAB || it == LF || it == '\r' }) return value
        val out = StringBuilder(value.length + 8)
        for (ch in value) {
            when (ch) {
                '\\' -> out.append("\\\\")
                TAB -> out.append("\\t")
                LF -> out.append("\\n")
                '\r' -> out.append("\\r")
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    fun unescape(value: String): String {
        if ('\\' !in value) return value
        val out = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch == '\\' && i + 1 < value.length) {
                out.append(decoded(value[i + 1]))
                i += 2
            } else {
                out.append(ch)
                i++
            }
        }
        return out.toString()
    }

    private fun decoded(code: Char): Char = when (code) {
        't' -> TAB
        'n' -> LF
        'r' -> '\r'
        else -> code
    }
}
