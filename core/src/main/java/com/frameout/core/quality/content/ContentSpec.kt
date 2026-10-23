package com.frameout.core.quality.content

data class ContentSpec(
    val millis: Long = DEFAULT_MILLIS,
    val strict: Int = STRICT_BALANCED,
    val gapMs: Long = DEFAULT_GAP_MS,
    val all: Boolean = false,
    val tags: Set<String> = emptySet()
) {
    val skipSteps: Int
        get() = if (gapMs <= 0L) 0 else (((gapMs + millis - 1L) / millis) - 1L).toInt().coerceAtLeast(0)

    fun toggled(id: String): ContentSpec = copy(tags = if (id in tags) tags - id else tags + id)

    fun encode(): String = "${millis.coerceAtLeast(1L)}:${strict.coerceIn(STRICT_LOOSE, STRICT_STRICT)}:${gapMs.coerceAtLeast(0L)}:${if (all) 1 else 0}:${tags.sorted().joinToString(",")}"

    companion object {
        const val STRICT_LOOSE = 0
        const val STRICT_BALANCED = 1
        const val STRICT_STRICT = 2
        const val DEFAULT_MILLIS = 1000L
        const val DEFAULT_GAP_MS = 2000L
        const val MAX_TAGS = 120
        val INTERVALS_MS = listOf(500L, 1000L, 2000L)
        val GAPS_MS = listOf(0L, 2000L, 5000L, 10000L)

        fun decode(value: String?): ContentSpec {
            val parts = value.orEmpty().split(':', limit = 5)
            if (parts.size < 5) return ContentSpec()
            val ids = parts[4].split(',').filter { it.isNotBlank() }.take(MAX_TAGS)
            return ContentSpec(
                millis = parts[0].toLongOrNull()?.coerceIn(1L, MAX_MILLIS) ?: DEFAULT_MILLIS,
                strict = parts[1].toIntOrNull()?.coerceIn(STRICT_LOOSE, STRICT_STRICT) ?: STRICT_BALANCED,
                gapMs = parts[2].toLongOrNull()?.coerceIn(0L, MAX_MILLIS) ?: DEFAULT_GAP_MS,
                all = parts[3] == "1",
                tags = ContentCatalog.known(ids)
            )
        }

        private const val MAX_MILLIS = 86_400_000L
    }
}
