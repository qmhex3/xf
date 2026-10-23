package com.frameout.designsystem.dock

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

internal object DockSpotMemory {
    var owner by mutableIntStateOf(0)
        private set

    private var nextId = 0
    private var reader: (() -> Pair<Float, Float>)? = null
    private var snapshot: Pair<Float, Float>? = null
    private var snapshotAt = 0L

    fun newId(): Int = ++nextId

    fun seed(): Pair<Float, Float>? {
        reader?.let { return it() }
        val s = snapshot ?: return null
        return if (SystemClock.uptimeMillis() - snapshotAt < SEED_WINDOW_MS) s else null
    }

    fun claim(id: Int, read: () -> Pair<Float, Float>) {
        if (id < owner) return
        reader = read
        if (owner != id) owner = id
    }

    fun leave(id: Int) {
        if (owner != id) return
        snapshot = reader?.invoke()
        snapshotAt = SystemClock.uptimeMillis()
        reader = null
    }

    private const val SEED_WINDOW_MS = 700L
}
