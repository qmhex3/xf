package com.frameout.engine.decode

import com.frameout.core.util.DiagLog
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

object DecodeQuality {
    data class Reduction(val count: Int, val sourceWidth: Int, val sourceHeight: Int, val outputWidth: Int, val outputHeight: Int)

    private data class Size(val sourceWidth: Int, val sourceHeight: Int, val outputWidth: Int, val outputHeight: Int)

    private val reduced = AtomicInteger(0)
    private val first = AtomicReference<Size?>(null)

    fun markReduced(reason: String, sourceWidth: Int = 0, sourceHeight: Int = 0, outputWidth: Int = 0, outputHeight: Int = 0) {
        if (reduced.incrementAndGet() == 1) {
            if (sourceWidth > 0 && outputWidth > 0) first.compareAndSet(null, Size(sourceWidth, sourceHeight, outputWidth, outputHeight))
            DiagLog.append("decode", reason)
        }
    }

    fun reset() {
        reduced.set(0)
        first.set(null)
    }

    fun takeReduced(): Int = takeReduction()?.count ?: 0

    fun takeReduction(): Reduction? {
        val count = reduced.getAndSet(0)
        val size = first.getAndSet(null)
        if (count <= 0) return null
        return Reduction(count, size?.sourceWidth ?: 0, size?.sourceHeight ?: 0, size?.outputWidth ?: 0, size?.outputHeight ?: 0)
    }
}
