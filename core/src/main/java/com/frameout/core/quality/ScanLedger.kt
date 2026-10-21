package com.frameout.core.quality

enum class ScanVerdict { Complete, Partial, Failed, Stopped }

class ScanProgress(val done: Int, val failed: Int, val total: Int)

class ScanReport(
    val total: Int,
    val analyzed: Int,
    val partial: Int,
    val failed: Int,
    val detectors: Map<String, Int>,
    val stopped: Boolean,
    val unavailable: List<String> = emptyList()
) {
    val incomplete: Int get() = partial + failed

    val verdict: ScanVerdict
        get() = when {
            stopped -> ScanVerdict.Stopped
            analyzed == 0 && partial == 0 && failed > 0 -> ScanVerdict.Failed
            incomplete > 0 -> ScanVerdict.Partial
            else -> ScanVerdict.Complete
        }

    val worstDetector: String? get() = unavailable.firstOrNull() ?: detectors.maxByOrNull { it.value }?.key

    companion object {
        val EMPTY = ScanReport(0, 0, 0, 0, emptyMap(), false)
    }
}

class ScanLedger(private val attempts: MutableMap<Long, Int>, private val maxAttempts: Int, private val breakerLimit: Int) {
    private val parkedIds = HashSet<Long>()
    private val detectors = HashMap<String, Int>()
    private val runs = HashMap<String, Int>()
    private var streak = 0

    var analyzed = 0
        private set
    var partial = 0
        private set
    var failed = 0
        private set

    val tripped: Boolean get() = streak >= breakerLimit
    val parkedCount: Int get() = parkedIds.size
    val done: Int get() = analyzed + partial

    fun isParked(id: Long): Boolean = id in parkedIds

    fun exhausted(id: Long): Boolean = (attempts[id] ?: 0) >= maxAttempts

    fun skip(id: Long) {
        if (parkedIds.add(id)) failed++
    }

    fun complete(id: Long) {
        attempts.remove(id)
        analyzed++
        streak = 0
        runs.clear()
    }

    fun markPartial(id: Long, names: List<String>): Boolean {
        count(names)
        streak = 0
        runs.keys.retainAll(names.toSet())
        names.forEach { name -> runs.merge(name, 1) { a, b -> a + b } }
        parkedIds += id
        partial++
        val used = attempts.merge(id, 1) { a, b -> a + b } ?: 1
        return used >= maxAttempts || names.all { (runs[it] ?: 0) >= breakerLimit }
    }

    fun allFailed(id: Long, names: List<String>) {
        count(names)
        streak++
        runs.clear()
        lose(id)
    }

    fun unreadable(id: Long, reason: String) {
        count(listOf(reason))
        lose(id)
    }

    fun report(total: Int, stopped: Boolean): ScanReport =
        ScanReport(total, analyzed, partial, failed, detectors.toMap(), stopped, runs.filterValues { it >= breakerLimit }.keys.sorted())

    private fun lose(id: Long) {
        attempts.merge(id, 1) { a, b -> a + b }
        parkedIds += id
        failed++
    }

    private fun count(names: List<String>) {
        names.forEach { name -> detectors.merge(name, 1) { a, b -> a + b } }
    }
}
