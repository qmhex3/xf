package com.frameout.core.quality.content

import com.frameout.core.quality.TagRules

object ContentMatcher {
    const val LABEL_FLOOR = 0.5f

    fun threshold(strict: Int): Float = when (strict) {
        ContentSpec.STRICT_LOOSE -> LABEL_FLOOR
        ContentSpec.STRICT_STRICT -> 0.75f
        else -> 0.6f
    }

    fun labelHits(tags: List<ContentTag>, scores: Map<String, Float>, min: Float): Set<String> =
        tags.filter { tag -> tag.labels.any { (scores[it] ?: 0f) >= min } }.mapTo(HashSet()) { it.id }

    fun faceHits(tags: List<ContentTag>, faces: Int, smiling: Int): Set<String> =
        tags.filter { faceHit(it.detector, faces, smiling) }.mapTo(HashSet()) { it.id }

    fun textHit(text: String): Boolean = text.count { !it.isWhitespace() } >= TagRules.MIN_TEXT_CHARS

    fun satisfied(spec: ContentSpec, hits: Set<String>): Boolean = when {
        spec.tags.isEmpty() -> true
        spec.all -> hits.containsAll(spec.tags)
        else -> spec.tags.any { it in hits }
    }

    private fun faceHit(detector: ContentDetector, faces: Int, smiling: Int): Boolean = when (detector) {
        ContentDetector.Face -> faces >= 1
        ContentDetector.Smile -> smiling >= 1
        ContentDetector.Group -> faces >= TagRules.GROUP_SIZE
        else -> false
    }
}
