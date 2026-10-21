package com.frameout.core.quality

import com.frameout.core.db.FrameEntity
import com.frameout.core.db.FrameScanEntity
import com.frameout.core.db.FrameTagDao
import com.frameout.core.db.FrameTagEntity
import com.frameout.core.util.DetectorHealth
import com.frameout.core.util.DeviceLimits
import com.frameout.core.util.DiagLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object FrameTagScanner {
    private const val BATCH = 16
    private const val BREAKER = 8
    private const val BLOCK_MS = 10L * 60L * 1000L
    const val MAX_ATTEMPTS = 2

    private val attempts = ConcurrentHashMap<Long, Int>()
    @Volatile private var blockedUntil = 0L

    private class Batch {
        val ids = ArrayList<Long>()
        val tags = ArrayList<FrameTagEntity>()
        val scans = ArrayList<FrameScanEntity>()
    }

    fun autoScanAllowed(now: Long = System.currentTimeMillis()): Boolean = now >= blockedUntil

    fun clearBlock() {
        blockedUntil = 0L
    }

    suspend fun scan(dao: FrameTagDao, projectId: Long, onProgress: suspend (ScanProgress) -> Unit): ScanReport =
        scan(DaoScanStore(dao), projectId, ThumbAnalyzer, attempts, onProgress)

    suspend fun scan(
        store: ScanStore,
        projectId: Long,
        analyzer: FrameAnalyzer,
        attempts: MutableMap<Long, Int>,
        onProgress: suspend (ScanProgress) -> Unit
    ): ScanReport {
        val total = store.countUnscanned(projectId)
        if (total == 0) return ScanReport.EMPTY
        val ledger = ScanLedger(attempts, MAX_ATTEMPTS, BREAKER)
        onProgress(ScanProgress(0, 0, total))
        while (!ledger.tripped) {
            val rows = store.unscanned(projectId, BATCH + ledger.parkedCount)
            rows.filter { ledger.exhausted(it.id) }.forEach { ledger.skip(it.id) }
            val frames = rows.filter { !ledger.isParked(it.id) }.take(BATCH)
            if (frames.isEmpty()) break
            val batch = Batch()
            withContext(Dispatchers.IO) {
                for (frame in frames) {
                    record(frame, projectId, analyzer, ledger, batch)
                    if (ledger.tripped) break
                }
            }
            store.commit(batch.ids, batch.tags, batch.scans)
            onProgress(ScanProgress(ledger.done.coerceAtMost(total), ledger.failed, total))
        }
        return finish(projectId, ledger.report(total, ledger.tripped))
    }

    private fun finish(projectId: Long, report: ScanReport): ScanReport {
        if (report.stopped) blockedUntil = System.currentTimeMillis() + BLOCK_MS
        if (report.verdict != ScanVerdict.Complete) {
            val down = if (report.unavailable.isEmpty()) "" else " unavailable=${report.unavailable.joinToString()}"
            val detail = report.detectors.entries.joinToString { "${it.key}=${it.value} (${DetectorHealth.lastError(it.key) ?: "-"})" }
            DiagLog.append("FrameTagScanner", "project=$projectId ${report.verdict} total=${report.total} analyzed=${report.analyzed} partial=${report.partial} failed=${report.failed}$down $detail")
        }
        return report
    }

    private suspend fun record(frame: FrameEntity, projectId: Long, analyzer: FrameAnalyzer, ledger: ScanLedger, out: Batch) {
        currentCoroutineContext().ensureActive()
        ScanThrottle.awaitIdle()
        DeviceLimits.scanCooldownMs().takeIf { it > 0L }?.let { delay(it) }
        val analysis = try {
            analyzer.analyze(frame, projectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: AllDetectorsFailed) {
            ledger.allFailed(frame.id, e.names)
            return
        } catch (e: Exception) {
            val reason = if (e is FrameUnreadable) "decode" else "analyze"
            DetectorHealth.failed(reason, e)
            ledger.unreadable(frame.id, reason)
            return
        }
        if (analysis == null) {
            ledger.unreadable(frame.id, "decode")
            return
        }
        val mark = if (analysis.failed.isEmpty()) completed(ledger, frame.id) else ledger.markPartial(frame.id, analysis.failed)
        out.ids += frame.id
        out.tags += analysis.tags
        if (mark) out.scans += FrameScanEntity(frame.id, projectId)
    }

    private fun completed(ledger: ScanLedger, id: Long): Boolean {
        ledger.complete(id)
        return true
    }

    suspend fun reset(dao: FrameTagDao, projectId: Long) {
        attempts.clear()
        clearBlock()
        dao.clearTags(projectId)
        dao.clearScans(projectId)
    }
}
