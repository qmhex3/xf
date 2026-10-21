package com.frameout.core.quality

import com.frameout.core.util.DetectorHealth
import kotlinx.coroutines.CancellationException

class AllDetectorsFailed(val names: List<String>) : IllegalStateException("all detectors failed: ${names.joinToString()}")

class TagOutcome(val hits: List<TagHit>, val failed: List<String> = emptyList()) {
    val failedDetectors: Int get() = failed.size
}

class DetectorRun(private val total: Int, private val report: (String, Throwable) -> Unit = DetectorHealth::failed) {
    val hits = ArrayList<TagHit>()
    val failed = ArrayList<String>()

    suspend fun step(name: String, block: suspend () -> List<TagHit>) {
        try {
            hits += block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed += name
            report(name, e)
        }
    }

    fun outcome(refine: (List<TagHit>) -> List<TagHit> = { it }): TagOutcome {
        if (failed.size >= total) throw AllDetectorsFailed(failed.toList())
        return TagOutcome(refine(hits), failed.toList())
    }
}
