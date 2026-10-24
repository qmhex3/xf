package com.frameout.core.quality.content

data class ContentSpec(
    val strict: Int = STRICT_BALANCED,
    val all: Boolean = false,
    val tags: Set<String> = emptySet()
) {
    fun toggled(id: String): ContentSpec = copy(tags = if (id in tags) tags - id else tags + id)

    fun encode(): String = "$LEGACY_STEP:${strict.coerceIn(STRICT_LOOSE, STRICT_STRICT)}:$LEGACY_GAP:${if (all) 1 else 0}:${tags.sorted().joinToString(",")}"

    companion object {
        const val STRICT_LOOSE = 0
        const val STRICT_BALANCED = 1
        const val STRICT_STRICT = 2
        const val MAX_TAGS = 120
        const val SAMPLE_STEP_MS = 200L
        private const val LEGACY_STEP = 1
        private const val LEGACY_GAP = 0

        fun decode(value: String?): ContentSpec {
            val parts = value.orEmpty().split(':', limit = 5)
            if (parts.size < 5) return ContentSpec()
            val ids = parts[4].split(',').filter { it.isNotBlank() }.take(MAX_TAGS)
            return ContentSpec(
                strict = parts[1].toIntOrNull()?.coerceIn(STRICT_LOOSE, STRICT_STRICT) ?: STRICT_BALANCED,
                all = parts[3] == "1",
                tags = ContentCatalog.known(ids)
            )
        }
    }
}
