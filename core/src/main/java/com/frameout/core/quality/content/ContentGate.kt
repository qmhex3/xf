package com.frameout.core.quality.content

import android.graphics.Bitmap
import com.frameout.core.quality.SkipReason

class ContentGate(private val spec: ContentSpec) {
    private val cooldown = ContentCooldown(spec.skipSteps)

    suspend fun accept(bitmap: Bitmap, onReject: (SkipReason) -> Unit): Boolean {
        if (spec.tags.isEmpty()) return true
        if (cooldown.blocked()) {
            onReject(SkipReason.Duplicate)
            return false
        }
        val hit = ContentFilter.matches(bitmap, spec)
        if (hit) cooldown.hit() else onReject(SkipReason.NoMatch)
        return hit
    }
}
