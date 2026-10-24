package com.frameout.core.quality.content

import android.graphics.Bitmap
import com.frameout.core.quality.FocusScore
import com.frameout.core.quality.FrameSignature
import com.frameout.core.quality.SkipReason
import com.frameout.core.quality.Sharpness
import com.frameout.core.util.SmoothScale

class ContentGate(private val spec: ContentSpec) {
    private var lastSig: ByteArray? = null
    private var lastNoise = 0f
    private var lastVerdict: Boolean? = null

    suspend fun accept(bitmap: Bitmap, onReject: (SkipReason) -> Unit): Boolean {
        if (spec.tags.isEmpty()) return true
        val look = signatureOf(bitmap)
        val previous = lastSig
        val known = lastVerdict
        val reused = if (look != null && previous != null && known != null && FrameSignature.same(previous, look.first, 1.0, lastNoise, look.second)) known else null
        val hit = reused ?: ContentFilter.matches(bitmap, spec)
        if (reused == null) {
            lastSig = look?.first
            lastNoise = look?.second ?: 0f
            lastVerdict = if (look != null) hit else null
        }
        if (!hit) onReject(SkipReason.NoMatch)
        return hit
    }

    private fun signatureOf(bitmap: Bitmap): Pair<ByteArray, Float>? = runCatching {
        if (bitmap.isRecycled) return@runCatching null
        val scale = SAMPLE_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
        val w = (bitmap.width * scale).toInt().coerceAtLeast(MIN_SIDE)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(MIN_SIDE)
        val small = if (w >= bitmap.width) bitmap else SmoothScale.scaled(bitmap, w, h, true)
        try {
            val luma = Sharpness.lumaOf(small)
            val sig = FrameSignature.of(luma, small.width, small.height) ?: return@runCatching null
            val noise = FrameSignature.cellNoise(FocusScore.detail(luma, small.width, small.height).noise, small.width, small.height)
            sig to noise
        } finally {
            if (small !== bitmap) small.recycle()
        }
    }.getOrNull()

    private companion object {
        const val SAMPLE_SIDE = 160
        const val MIN_SIDE = 16
    }
}
