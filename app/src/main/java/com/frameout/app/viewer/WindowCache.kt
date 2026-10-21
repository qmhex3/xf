package com.frameout.app.viewer

internal class WindowCache<V : Any>(
    private val budgetBytes: Long,
    private val maxEntryBytes: Long,
    private val sizeOf: (V) -> Long,
    private val drop: (V) -> Unit
) {
    private class Slot<V>(val tag: Any?, val value: V, val bytes: Long)

    private val slots = HashMap<Long, Slot<V>>()
    private var ranks: Map<Long, Int> = emptyMap()
    private var used = 0L

    val usedBytes: Long get() = used
    val size: Int get() = slots.size

    fun contains(id: Long) = id in slots
    fun wants(id: Long) = ranks.containsKey(id)

    fun take(id: Long, tag: Any?): V? {
        val slot = slots.remove(id) ?: return null
        used -= slot.bytes
        if (slot.tag == tag) return slot.value
        drop(slot.value)
        return null
    }

    fun setWindow(ids: List<Long>) {
        ranks = ids.withIndex().associate { it.value to it.index }
        slots.keys.filter { it !in ranks }.forEach { remove(it) }
    }

    fun put(id: Long, tag: Any?, value: V): Boolean {
        val bytes = sizeOf(value)
        if (bytes > maxEntryBytes || bytes > budgetBytes) { drop(value); return false }
        remove(id)
        val mine = rankOf(id)
        while (used + bytes > budgetBytes) {
            val worst = slots.keys.maxByOrNull { rankOf(it) } ?: break
            if (rankOf(worst) <= mine) { drop(value); return false }
            remove(worst)
        }
        slots[id] = Slot(tag, value, bytes)
        used += bytes
        return true
    }

    fun clear() = slots.keys.toList().forEach { remove(it) }

    private fun rankOf(id: Long) = ranks[id] ?: ranks.size

    private fun remove(id: Long) {
        val slot = slots.remove(id) ?: return
        used -= slot.bytes
        drop(slot.value)
    }
}
