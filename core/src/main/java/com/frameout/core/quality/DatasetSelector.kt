package com.frameout.core.quality

data class DatasetOptions(
    val intervalUs: Long = ONE_SECOND_US,
    val blurLevel: BlurLevel = BlurLevel.Normal,
    val dropDuplicates: Boolean = true,
    val keepRejected: Boolean = false
) {
    companion object {
        const val ONE_SECOND_US = 1_000_000L
    }
}

enum class BlurLevel(val floorShare: Double) {
    Off(0.0),
    Normal(0.25),
    Strict(0.5)
}

data class DatasetPick(val id: Long, val index: Int, val ptsUs: Long, val score: Double, val sharpness: Int)

class DatasetResult(val kept: List<DatasetPick>, val rejected: List<DatasetPick>)

object DatasetSelector {
    private const val REFERENCE_PERCENTILE = 0.9

    fun select(frames: List<ScoredFrame>, options: DatasetOptions): List<DatasetPick> = split(frames, options).kept

    fun split(frames: List<ScoredFrame>, options: DatasetOptions): DatasetResult {
        if (frames.isEmpty()) return DatasetResult(emptyList(), emptyList())
        val ordered = frames.sortedWith(compareBy({ it.ptsUs }, { it.index }))
        val reference = reference(ordered)
        val floor = reference * options.blurLevel.floorShare
        val best = windows(ordered, options.intervalUs.coerceAtLeast(1L)).map { window -> window.maxBy { it.effective } }
        val (sharp, blurry) = best.partition { it.effective > 0.0 && it.effective >= floor }
        val kept = if (options.dropDuplicates) withoutRepeats(sharp) else sharp
        return DatasetResult(kept.map { pick(it, reference) }, blurry.map { pick(it, reference) })
    }

    private fun pick(f: ScoredFrame, reference: Double) = DatasetPick(f.id, f.index, f.ptsUs, f.effective, sharpness(f.effective, reference))

    fun sharpness(score: Double, reference: Double): Int =
        if (reference <= 0.0) 0 else (score / reference * 100.0).toInt().coerceIn(0, 100)

    fun reference(frames: List<ScoredFrame>): Double {
        val scores = frames.map { it.effective }.filter { it > 0.0 }.sorted()
        if (scores.isEmpty()) return 0.0
        return scores[((scores.size - 1) * REFERENCE_PERCENTILE).toInt()]
    }

    private fun windows(ordered: List<ScoredFrame>, intervalUs: Long): List<List<ScoredFrame>> {
        val start = ordered.first().ptsUs
        return ordered.groupBy { (it.ptsUs - start) / intervalUs }.toSortedMap().values.toList()
    }

    private fun withoutRepeats(winners: List<ScoredFrame>): List<ScoredFrame> {
        val kept = ArrayList<ScoredFrame>(winners.size)
        for (w in winners) {
            val last = kept.lastOrNull()
            if (last != null && repeats(last, w)) {
                if (w.effective > last.effective) kept[kept.size - 1] = w
            } else kept += w
        }
        return kept
    }

    private fun repeats(a: ScoredFrame, b: ScoredFrame): Boolean {
        val sa = a.sig ?: return false
        val sb = b.sig ?: return false
        return FrameSignature.same(sa, sb, 1.0, a.noise, b.noise)
    }
}
