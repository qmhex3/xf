package com.frameout.engine.edit

data class KeptRange(
    val startUs: Long,
    val endUs: Long
)

data class ReassemblePlan(
    val sourceUri: String,
    val outputPath: String,
    val keptRanges: List<KeptRange>
) {
    val isEmpty: Boolean get() = keptRanges.isEmpty()
}

sealed class ReassembleResult {
    data class Success(val path: String, val sizeBytes: Long, val reencoded: Boolean = false) : ReassembleResult()
    data class Failure(val reason: String) : ReassembleResult()
}

fun buildKeptRanges(activePtsUs: List<Long>, gapUs: Long = 50_000L, keyframeAtOrBefore: ((Long) -> Long?)? = null): List<KeptRange> {
    if (activePtsUs.isEmpty()) return emptyList()
    val sorted = activePtsUs.sorted()
    val ranges = mutableListOf<KeptRange>()
    var start = sorted.first()
    var prev = sorted.first()
    for (i in 1 until sorted.size) {
        val pts = sorted[i]
        if (pts - prev > gapUs) {
            ranges += KeptRange(start, prev + 1)
            start = pts
        }
        prev = pts
    }
    ranges += KeptRange(start, prev + 1)
    return if (keyframeAtOrBefore == null) ranges else alignRangesToKeyframes(ranges, keyframeAtOrBefore) ?: ranges
}

fun alignRangesToKeyframes(ranges: List<KeptRange>, keyframeAtOrBefore: (Long) -> Long?): List<KeptRange>? {
    val aligned = ArrayList<KeptRange>(ranges.size)
    for (range in ranges.sortedBy { it.startUs }) {
        val start = keyframeAtOrBefore(range.startUs) ?: return null
        val last = aligned.lastOrNull()
        if (last != null && start <= last.endUs) {
            aligned[aligned.lastIndex] = KeptRange(last.startUs, maxOf(last.endUs, range.endUs))
        } else {
            aligned += KeptRange(minOf(start, range.startUs), range.endUs)
        }
    }
    return aligned
}

fun KeptRange.contains(ptsUs: Long): Boolean = ptsUs >= startUs && ptsUs < endUs

fun List<KeptRange>.containsPts(ptsUs: Long): Boolean = any { it.contains(ptsUs) }
