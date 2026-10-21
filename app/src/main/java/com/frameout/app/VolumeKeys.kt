package com.frameout.app

import android.view.KeyEvent

interface VolumeTarget {
    fun isActive(): Boolean
    fun onStep(forward: Boolean)
}

object VolumeKeys {
    @Volatile
    private var target: VolumeTarget? = null

    fun direction(keyCode: Int): Boolean? = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> true
        KeyEvent.KEYCODE_VOLUME_DOWN -> false
        else -> null
    }

    fun register(value: VolumeTarget) {
        target = value
    }

    fun unregister(value: VolumeTarget) {
        if (target === value) target = null
    }

    fun dispatch(event: KeyEvent): Boolean {
        val current = target ?: return false
        val forward = direction(event.keyCode) ?: return false
        if (!current.isActive()) return false
        val canceled = event.flags and KeyEvent.FLAG_CANCELED != 0
        if (event.action == KeyEvent.ACTION_DOWN && !canceled) current.onStep(forward)
        return true
    }
}
