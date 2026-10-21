package com.frameout.app.viewer

import com.frameout.core.quality.ScannedWord

data class LiveTextState(
    val active: Boolean = false,
    val scanning: Boolean = false,
    val scanned: Boolean = false,
    val failed: Boolean = false,
    val words: List<ScannedWord> = emptyList(),
    val anchor: Int = -1,
    val focus: Int = -1
) {
    val hasSelection: Boolean get() = words.isNotEmpty() && anchor in words.indices && focus in words.indices

    val range: IntRange
        get() = if (hasSelection) minOf(anchor, focus)..maxOf(anchor, focus) else IntRange.EMPTY

    fun isSelected(index: Int): Boolean = hasSelection && index in range

    fun selectedText(): String {
        if (!hasSelection) return ""
        val sb = StringBuilder()
        var previousLine = -1
        for (i in range) {
            val w = words[i]
            if (previousLine != -1) sb.append(if (w.line != previousLine) '\n' else ' ')
            sb.append(w.text)
            previousLine = w.line
        }
        return sb.toString()
    }
}
