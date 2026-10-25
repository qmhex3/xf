package com.frameout.core.quality

import com.frameout.core.util.SmoothScale
import android.graphics.Bitmap

class ExtractQualityGate(private val skipBlurry: Boolean, private val skipDuplicates: Boolean, private val peopleOnly: Boolean = false, private val includeBody: Boolean = true, private val content: com.frameout.core.quality.content.ContentGate? = null, private val blockAware: Boolean = false, private val onReject: (SkipReason) -> Unit = {}) {
    private val recent = ArrayDeque<Double>()
    private var lastSig: ByteArray? = null
    private var lastNoise = 0f

    fun accept(bitmap: Bitmap): Boolean {
        val blocks = measuredFactor(bitmap)
        return withSample(bitmap) { luma, w, h -> accept(luma, w, h, blocks) }
    }

    fun seed(bitmap: Bitmap, blockFactor: Double = 1.0) {
        withSample(bitmap) { luma, w, h -> seed(luma, w, h, blockFactor) }
    }

    fun seed(luma: IntArray, width: Int, height: Int, blockFactor: Double = 1.0) {
        remember(FocusScore.detail(luma, width, height), blockFactor)
        if (skipDuplicates) {
            lastSig = FrameSignature.of(luma, width, height)
            lastNoise = FrameSignature.cellNoise(FocusScore.detail(luma, width, height).noise, width, height)
        }
    }

    private fun measuredFactor(bitmap: Bitmap): Double = if (blockAware && skipBlurry) BlockProbe.factor(bitmap) else 1.0

    private inline fun <T> withSample(bitmap: Bitmap, block: (IntArray, Int, Int) -> T): T {
        val scale = SAMPLE_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
        val w = (bitmap.width * scale).toInt().coerceAtLeast(MIN_SIDE)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(MIN_SIDE)
        val small = if (w >= bitmap.width) bitmap else SmoothScale.scaled(bitmap, w, h, true)
        return try {
            block(Sharpness.lumaOf(small), small.width, small.height)
        } finally {
            if (small !== bitmap) small.recycle()
        }
    }

    suspend fun acceptAsync(bitmap: Bitmap): Boolean = accept(bitmap) && acceptPeople(bitmap) && (content?.accept(bitmap, onReject) ?: true)

    private suspend fun acceptPeople(bitmap: Bitmap): Boolean =
        !peopleOnly || PeopleFilter.containsPerson(bitmap, includeBody).also { if (!it) onReject(SkipReason.NoPeople) }

    fun accept(luma: IntArray, width: Int, height: Int, blockFactor: Double = 1.0): Boolean {
        val detail = FocusScore.detail(luma, width, height)
        val score = detail.score * blockFactor
        val baseline = median()
        val enough = recent.size >= MIN_HISTORY
        if (!detail.plain) {
            val blurry = skipBlurry && enough && baseline > 0.0 && score < baseline * BLUR_RATIO
            remember(detail, blockFactor)
            if (blurry) { onReject(SkipReason.Blurry); return false }
        }
        if (skipDuplicates) {
            val sig = FrameSignature.of(luma, width, height)
            val last = lastSig
            val noise = FrameSignature.cellNoise(detail.noise, width, height)
            if (sig != null && last != null && FrameSignature.same(last, sig, 1.0, lastNoise, noise)) { onReject(SkipReason.Duplicate); return false }
            lastSig = sig
            lastNoise = noise
        }
        return true
    }

    private fun remember(detail: FocusScore.Detail, blockFactor: Double) {
        if (detail.plain) return
        recent.addLast(detail.score * blockFactor)
        if (recent.size > HISTORY) recent.removeFirst()
    }

    private fun median(): Double {
        if (recent.isEmpty()) return 0.0
        val sorted = recent.sorted()
        return sorted[sorted.size / 2]
    }

    companion object {
        const val SAMPLE_SIDE = 160
        const val MIN_SIDE = 16
        const val HISTORY = 31
        const val MIN_HISTORY = 8
        const val BLUR_RATIO = 0.4
    }
}
