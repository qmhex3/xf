package com.frameout.core.quality

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object FrameImportance {
    const val MIN_FRAMES = 8
    const val MIN_PICKS = 3
    const val MAX_PICKS = 30

    private const val PERSON_PREFIX = "p:"
    private const val CONCEPT_MIN = 0.6f
    private const val MIN_TAGGED_SHARE = 0.5

    private const val W_RARITY = 0.45
    private const val W_MOMENT = 0.30
    private const val W_SUBJECT = 0.25
    private const val BASE = 0.15
    private const val GATE_FLOOR = 0.25

    private const val SMILE_WEIGHT = 0.8
    private const val FACE_WEIGHT = 0.35
    private const val ACTION_WEIGHT = 0.85
    private const val EYES_CLOSED_MIN = 0.5
    private const val EYES_CLOSED_FACTOR = 0.6

    private const val SUBJECT_GAIN = 1.6
    private const val CENTER_PENALTY = 0.4
    private const val HALF_DIAGONAL = 0.7071

    private const val LAMBDA = 0.7
    private const val STOP_RATIO = 0.35
    private const val BUCKETS_PER_PICK = 4
    private const val REDUNDANT = 0.85
    private const val TAG_FLOOR = 0.4
    private const val TAG_REACH = 3.0

    private class Model(val importance: DoubleArray, val weights: List<Map<String, Double>>)

    fun maxPicks(count: Int): Int = sqrt(count.toDouble()).roundToInt().coerceIn(MIN_PICKS, MAX_PICKS)

    fun pick(frames: List<ScoredFrame>, tags: Map<Long, List<TagHit>>): List<Long> {
        val sorted = frames.sortedBy { it.index }
        val model = model(sorted, tags) ?: return SharpestPicker.pick(sorted)
        return select(sorted, model).sorted().map { sorted[it].id }
    }

    internal fun scores(frames: List<ScoredFrame>, tags: Map<Long, List<TagHit>>): List<Double>? =
        model(frames.sortedBy { it.index }, tags)?.importance?.toList()

    private fun model(sorted: List<ScoredFrame>, tags: Map<Long, List<TagHit>>): Model? {
        val n = sorted.size
        if (n < MIN_FRAMES) return null
        if (sorted.count { tags.containsKey(it.id) } < n * MIN_TAGGED_SHARE) return null

        val hits = sorted.map { SceneReasoner.refine(tags[it.id].orEmpty()) }
        val concepts = hits.map { concepts(it) }
        val idf = idf(concepts, n)

        val rarity = percentile(DoubleArray(n) { rarity(it, concepts, idf) })
        val quality = percentile(DoubleArray(n) { sorted[it].score * MotionEstimate.factor(sorted[it].speed) })
        val action = percentile(actionChange(hits))

        val importance = DoubleArray(n) { i ->
            val gate = GATE_FLOOR + (1.0 - GATE_FLOOR) * quality[i]
            val moment = max(faceMoment(hits[i]), ACTION_WEIGHT * action[i])
            val content = W_RARITY * rarity[i] + W_MOMENT * moment + W_SUBJECT * subject(hits[i])
            gate * (BASE + content) * eyesClosedFactor(hits[i])
        }
        val weights = concepts.map { c -> c.keys.associateWith { idf[it] ?: 0.0 } }
        return Model(importance, weights)
    }

    private fun concepts(hits: List<TagHit>): Map<String, Float> =
        hits.filter { !it.tag.startsWith(PERSON_PREFIX) && it.confidence >= CONCEPT_MIN }.associate { it.tag to it.confidence }

    private fun idf(concepts: List<Map<String, Float>>, n: Int): Map<String, Double> {
        val frames = HashMap<String, Int>()
        for (c in concepts) for (tag in c.keys) frames[tag] = (frames[tag] ?: 0) + 1
        return frames.mapValues { (_, count) -> ln((n + 1.0) / (count + 1.0)) }
    }

    private fun rarity(i: Int, concepts: List<Map<String, Float>>, idf: Map<String, Double>): Double {
        var sum = 0.0
        for ((tag, confidence) in concepts[i]) {
            val persistent = (i > 0 && tag in concepts[i - 1]) || (i < concepts.size - 1 && tag in concepts[i + 1])
            if (persistent) sum += confidence * (idf[tag] ?: 0.0)
        }
        return sum
    }

    private fun confidence(hits: List<TagHit>, tag: String): Double = hits.firstOrNull { it.tag == tag }?.confidence?.toDouble() ?: 0.0

    private fun faceMoment(hits: List<TagHit>): Double = max(
        confidence(hits, TagRules.GOOD_FACE),
        max(SMILE_WEIGHT * confidence(hits, TagRules.SMILE), FACE_WEIGHT * confidence(hits, TagRules.FACE))
    )

    private fun eyesClosedFactor(hits: List<TagHit>): Double =
        if (confidence(hits, TagRules.EYES_CLOSED) >= EYES_CLOSED_MIN && confidence(hits, TagRules.GOOD_FACE) == 0.0) EYES_CLOSED_FACTOR else 1.0

    private fun actionChange(hits: List<List<TagHit>>): DoubleArray {
        val boxes = hits.map { h -> h.firstOrNull { it.tag == TagRules.BODY }?.boxes?.maxByOrNull { it.width * it.height } }
        return DoubleArray(boxes.size) { i ->
            var change = 0.0
            val here = boxes[i]
            if (here != null) {
                if (i > 0) boxes[i - 1]?.let { change = max(change, 1.0 - SceneReasoner.iou(here, it)) }
                if (i < boxes.size - 1) boxes[i + 1]?.let { change = max(change, 1.0 - SceneReasoner.iou(here, it)) }
            }
            change
        }
    }

    private fun subject(hits: List<TagHit>): Double {
        var best = 0.0
        for (hit in hits) {
            if (hit.tag != TagRules.FACE && hit.tag != TagRules.BODY && !hit.tag.startsWith(TagRules.OBJECT)) continue
            for (box in hit.boxes) {
                val area = (box.width * box.height).toDouble().coerceIn(0.0, 1.0)
                val dx = (box.l + box.r) / 2.0 - 0.5
                val dy = (box.t + box.b) / 2.0 - 0.5
                val centre = min(1.0, sqrt(dx * dx + dy * dy) / HALF_DIAGONAL)
                best = max(best, min(1.0, SUBJECT_GAIN * sqrt(area)) * (1.0 - CENTER_PENALTY * centre))
            }
        }
        return best
    }

    private fun percentile(values: DoubleArray): DoubleArray {
        val positive = values.filter { it > 0.0 }.sorted().toDoubleArray()
        if (positive.isEmpty()) return DoubleArray(values.size)
        return DoubleArray(values.size) { i -> if (values[i] <= 0.0) 0.0 else upperBound(positive, values[i]).toDouble() / positive.size }
    }

    private fun upperBound(sorted: DoubleArray, value: Double): Int {
        var lo = 0
        var hi = sorted.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sorted[mid] <= value) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private fun select(sorted: List<ScoredFrame>, model: Model): List<Int> {
        val n = sorted.size
        val limit = maxPicks(n)
        val top = model.importance.maxOrNull() ?: return emptyList()
        if (top <= 0.0) return emptyList()

        val buckets = min(n, limit * BUCKETS_PER_PICK)
        val remaining = ArrayList<Int>(buckets)
        for (b in 0 until buckets) {
            val from = b * n / buckets
            val to = (b + 1) * n / buckets
            var best = from
            for (i in from until to) if (model.importance[i] > model.importance[best]) best = i
            remaining += best
        }

        val slot = max(1.0, n.toDouble() / limit)
        val chosen = ArrayList<Int>()
        var firstValue = 0.0
        while (chosen.size < limit && remaining.isNotEmpty()) {
            var bestAt = -1
            var bestValue = Double.NEGATIVE_INFINITY
            for ((at, candidate) in remaining.withIndex()) {
                val similarity = similarity(candidate, chosen, sorted, model, slot) ?: continue
                val value = LAMBDA * model.importance[candidate] / top - (1.0 - LAMBDA) * similarity
                if (value > bestValue) {
                    bestValue = value
                    bestAt = at
                }
            }
            if (bestAt < 0) break
            if (chosen.isEmpty()) firstValue = bestValue
            else if (chosen.size >= MIN_PICKS && bestValue < STOP_RATIO * firstValue) break
            chosen += remaining.removeAt(bestAt)
        }
        return chosen
    }

    private fun similarity(candidate: Int, chosen: List<Int>, sorted: List<ScoredFrame>, model: Model, slot: Double): Double? {
        var worst = 0.0
        for (other in chosen) {
            val a = sorted[candidate].sig
            val b = sorted[other].sig
            if (a != null && b != null && FrameSignature.same(a, b, 1.0, sorted[candidate].noise, sorted[other].noise)) return null
            val gap = abs(candidate - other) / slot
            val time = max(0.0, 1.0 - gap)
            val near = max(0.0, 1.0 - gap / TAG_REACH)
            val tags = tagSimilarity(model.weights[candidate], model.weights[other]) * (TAG_FLOOR + (1.0 - TAG_FLOOR) * near)
            val similar = max(time, tags)
            if (similar >= REDUNDANT) return null
            worst = max(worst, similar)
        }
        return worst
    }

    private fun tagSimilarity(a: Map<String, Double>, b: Map<String, Double>): Double {
        var shared = 0.0
        var total = 0.0
        for ((tag, weight) in a) {
            val other = b[tag]
            if (other != null) {
                shared += min(weight, other)
                total += max(weight, other)
            } else total += weight
        }
        for ((tag, weight) in b) if (tag !in a) total += weight
        return if (total <= 0.0) 0.0 else shared / total
    }
}
