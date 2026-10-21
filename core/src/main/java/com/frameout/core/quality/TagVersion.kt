package com.frameout.core.quality

interface TagVersionStore {
    fun stored(): Int
    fun save(version: Int)
}

object TagVersion {
    suspend fun ensure(store: TagVersionStore, current: Int, resetAll: suspend () -> Unit): Boolean {
        if (store.stored() >= current) return false
        resetAll()
        store.save(current)
        return true
    }
}
