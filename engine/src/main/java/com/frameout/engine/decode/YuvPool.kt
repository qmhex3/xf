package com.frameout.engine.decode

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

internal object YuvPool {
    private const val PARALLEL_MIN_PIXELS = 400_000L
    private const val MIN_BAND_ROWS = 64
    private const val MAX_WORKERS = 4
    private val workers = Runtime.getRuntime().availableProcessors().coerceIn(1, MAX_WORKERS)
    private val pool: ExecutorService by lazy {
        val counter = AtomicInteger()
        Executors.newFixedThreadPool(workers) { task ->
            Thread(task, "yuv-convert-${counter.incrementAndGet()}").apply { isDaemon = true }
        }
    }

    fun bands(width: Int, height: Int): Int {
        if (width.toLong() * height < PARALLEL_MIN_PIXELS) return 1
        return minOf(workers, height / MIN_BAND_ROWS).coerceAtLeast(1)
    }

    fun run(bands: Int, height: Int, render: (first: Int, last: Int) -> Unit) {
        if (bands <= 1) {
            render(0, height)
            return
        }
        val per = ((height + bands - 1) / bands + 1) / 2 * 2
        val tasks = ArrayList<Callable<Unit>>(bands)
        var first = 0
        while (first < height) {
            val from = first
            val to = minOf(height, first + per)
            tasks += Callable { render(from, to) }
            first = to
        }
        for (future in pool.invokeAll(tasks)) {
            try {
                future.get()
            } catch (e: ExecutionException) {
                throw e.cause ?: e
            }
        }
    }
}
