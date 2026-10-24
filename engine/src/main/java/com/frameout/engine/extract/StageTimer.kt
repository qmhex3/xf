package com.frameout.engine.extract

import java.util.concurrent.atomic.AtomicLongArray

internal class StageTimer {
    enum class Stage { Decode, Write, Commit }

    private val nanos = AtomicLongArray(Stage.entries.size)
    private val counts = AtomicLongArray(Stage.entries.size)
    private val startedAt = System.nanoTime()

    inline fun <T> time(stage: Stage, block: () -> T): T {
        val begin = System.nanoTime()
        try {
            return block()
        } finally {
            add(stage, System.nanoTime() - begin)
        }
    }

    @PublishedApi
    internal fun add(stage: Stage, elapsedNanos: Long) {
        nanos.addAndGet(stage.ordinal, elapsedNanos)
        counts.incrementAndGet(stage.ordinal)
    }

    fun summary(frames: Int, decoders: Int, writers: Int): String {
        val wallMs = (System.nanoTime() - startedAt) / NANOS_PER_MS
        val parts = Stage.entries.joinToString(" ") { stage ->
            val total = nanos.get(stage.ordinal) / NANOS_PER_MS
            val count = counts.get(stage.ordinal)
            "${stage.name.lowercase()}=${total}ms/${count}"
        }
        return "timing wall=${wallMs}ms frames=$frames decoders=$decoders writers=$writers $parts"
    }

    private companion object {
        const val NANOS_PER_MS = 1_000_000L
    }
}
