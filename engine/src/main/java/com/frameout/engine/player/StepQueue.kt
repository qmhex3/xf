package com.frameout.engine.player

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.sync.Mutex

class StepQueue(private val apply: suspend (Int) -> Unit) {
    private val pending = AtomicInteger(0)
    private val lock = Mutex()

    suspend fun push(delta: Int) {
        pending.addAndGet(delta)
        while (lock.tryLock()) {
            try {
                drain()
            } finally {
                lock.unlock()
            }
            if (pending.get() == 0) return
        }
    }

    private suspend fun drain() {
        while (true) {
            val delta = pending.getAndSet(0)
            if (delta == 0) return
            apply(delta)
        }
    }
}
