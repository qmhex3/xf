package com.frameout.core.quality

import com.frameout.core.db.FrameEntity
import com.frameout.core.db.FrameScanEntity
import com.frameout.core.db.FrameTagEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameTagScannerTest {
    private class FakeStore(count: Int) : ScanStore {
        private val frames = (1..count).map { FrameEntity(id = it.toLong(), projectId = 1, index = it, ptsUs = it * 1000L, thumbnailPath = "t$it") }
        val pending = frames.map { it.id }.toMutableSet()
        val tags = ArrayList<FrameTagEntity>()

        override suspend fun countUnscanned(projectId: Long): Int = pending.size

        override suspend fun unscanned(projectId: Long, limit: Int): List<FrameEntity> = frames.filter { it.id in pending }.take(limit)

        override suspend fun commit(frameIds: List<Long>, tags: List<FrameTagEntity>, scans: List<FrameScanEntity>) {
            this.tags += tags
            scans.forEach { pending -= it.frameId }
        }
    }

    private class Script(val result: (FrameEntity) -> FrameAnalysis?) : FrameAnalyzer {
        val seen = ArrayList<Long>()

        override suspend fun analyze(frame: FrameEntity, projectId: Long): FrameAnalysis? {
            seen += frame.id
            return result(frame)
        }
    }

    private fun tag(frame: FrameEntity) = FrameTagEntity(frameId = frame.id, projectId = 1, tag = "l:x", confidence = 1f)

    private fun good() = Script { FrameAnalysis(listOf(tag(it)), emptyList()) }

    private fun partial(vararg names: String) = Script { FrameAnalysis(listOf(tag(it)), names.toList()) }

    private fun run(store: FakeStore, analyzer: FrameAnalyzer, attempts: MutableMap<Long, Int> = HashMap(), progress: MutableList<ScanProgress> = ArrayList()): ScanReport =
        runBlocking { FrameTagScanner.scan(store, 1, analyzer, attempts) { progress += it } }

    @After
    fun unblock() = FrameTagScanner.clearBlock()

    @Test
    fun healthyScanMarksEveryFrameAndReachesTotal() {
        val store = FakeStore(4)
        val progress = ArrayList<ScanProgress>()
        val report = run(store, good(), progress = progress)
        assertEquals(ScanVerdict.Complete, report.verdict)
        assertEquals(4, report.analyzed)
        assertTrue(store.pending.isEmpty())
        assertEquals(4, progress.last().done)
        assertEquals(0, progress.last().failed)
    }

    @Test
    fun unreadableFramesAreFailuresNotProgress() {
        val store = FakeStore(5)
        val progress = ArrayList<ScanProgress>()
        val report = run(store, Script { throw FrameUnreadable("x") }, progress = progress)
        assertEquals(ScanVerdict.Failed, report.verdict)
        assertEquals(5, report.failed)
        assertEquals(0, report.analyzed)
        assertEquals(5, store.pending.size)
        assertEquals(0, progress.last().done)
        assertEquals(5, progress.last().failed)
    }

    @Test
    fun missingThumbnailIsAFailureToo() {
        val store = FakeStore(3)
        val report = run(store, Script { null })
        assertEquals(3, report.failed)
        assertEquals(3, store.pending.size)
    }

    @Test
    fun everyDetectorFailingStopsTheScanAndBlocksAutoRestart() {
        val store = FakeStore(30)
        val analyzer = Script { throw AllDetectorsFailed(listOf("face", "labels")) }
        val report = run(store, analyzer)
        assertEquals(ScanVerdict.Stopped, report.verdict)
        assertTrue(analyzer.seen.size < 30)
        assertEquals(30, store.pending.size)
        assertFalse(FrameTagScanner.autoScanAllowed())
        assertEquals(setOf("face", "labels"), report.detectors.keys)
    }

    @Test
    fun clearingTheBlockAllowsAutoScanAgain() {
        run(FakeStore(30), Script { throw AllDetectorsFailed(listOf("face")) })
        FrameTagScanner.clearBlock()
        assertTrue(FrameTagScanner.autoScanAllowed())
    }

    @Test
    fun partialFramesKeepTheirTagsButAreRetriedOnce() {
        val store = FakeStore(3)
        val attempts = HashMap<Long, Int>()
        val first = run(store, partial("labels"), attempts)
        assertEquals(3, first.partial)
        assertEquals(ScanVerdict.Partial, first.verdict)
        assertEquals(3, store.pending.size)
        assertEquals(3, store.tags.size)
        val second = run(store, partial("labels"), attempts)
        assertEquals(3, second.partial)
        assertTrue(store.pending.isEmpty())
    }

    @Test
    fun aDetectorFailingOnEveryFrameStopsBeingRetried() {
        val store = FakeStore(12)
        val report = run(store, partial("labels"))
        assertEquals(listOf("labels"), report.unavailable)
        assertEquals(12, report.partial)
        assertEquals(7, store.pending.size)
        assertEquals("labels", report.worstDetector)
    }

    @Test
    fun framesThatAlreadyFailedTwiceAreNotAnalyzedAgain() {
        val store = FakeStore(3)
        val analyzer = good()
        val report = run(store, analyzer, hashMapOf(2L to FrameTagScanner.MAX_ATTEMPTS))
        assertFalse(analyzer.seen.contains(2L))
        assertEquals(1, report.failed)
        assertEquals(2, report.analyzed)
        assertEquals(setOf(2L), store.pending)
    }

    @Test
    fun cleanFramesAfterAPartialOneStillComplete() {
        val store = FakeStore(4)
        val analyzer = Script { frame -> FrameAnalysis(listOf(tag(frame)), if (frame.id == 2L) listOf("pose") else emptyList()) }
        val report = run(store, analyzer)
        assertEquals(3, report.analyzed)
        assertEquals(1, report.partial)
        assertEquals(setOf(2L), store.pending)
    }

    @Test
    fun emptyProjectReturnsAnEmptyCompleteReport() {
        val report = run(FakeStore(0), good())
        assertEquals(ScanVerdict.Complete, report.verdict)
        assertEquals(0, report.total)
    }
}
