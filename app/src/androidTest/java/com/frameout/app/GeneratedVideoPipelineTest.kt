package com.frameout.app

import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.core.db.AppDatabase
import com.frameout.core.db.ProjectEntity
import com.frameout.core.storage.StorageManager
import com.frameout.engine.extract.CheckpointStore
import com.frameout.engine.extract.ExtractEngine
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractProgress
import com.frameout.engine.extract.ExtractRequest
import com.frameout.engine.index.MediaIndexer
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeneratedVideoPipelineTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = AppDatabase.get(context)
    private val storage = StorageManager(context)
    private val files = mutableListOf<File>()
    private val projects = mutableListOf<Long>()

    @After
    fun cleanup() = runBlocking {
        projects.forEach {
            db.videoIndexDao().clearIndex(it)
            db.projectDao().delete(it)
            storage.deleteProject(it)
        }
        files.forEach { it.delete() }
    }

    private fun generate(spec: VideoSpec): File {
        val file = File(context.cacheDir, "generated_${System.nanoTime()}.mp4").also { files += it }
        GeneratedVideo.write(file, spec)
        return file
    }

    private suspend fun project(file: File): Long {
        val now = System.currentTimeMillis()
        val uri = Uri.fromFile(file).toString()
        return db.projectDao().insert(ProjectEntity(name = "gen", sourceUri = uri, sourceName = file.name, createdAt = now, updatedAt = now)).also { projects += it }
    }

    private suspend fun index(file: File): Pair<Long, List<Long>> {
        val id = project(file)
        MediaIndexer(context, db).indexToDatabase(Uri.fromFile(file), id, db.videoIndexDao(), { false }, { false })
        return id to db.videoIndexDao().list(id).map { it.ptsUs }
    }

    private companion object {
        const val MUXER_ROUNDING_US = 200L
    }

    private fun recentLog(): String = runCatching {
        com.frameout.core.util.DiagLog.flush(3_000L)
        com.frameout.core.util.DiagLog.export(context).readText().takeLast(3000)
    }.getOrElse { "(diag log unavailable: ${it.message})" }

    private fun relative(values: List<Long>): List<Long> = values.sorted().let { sorted -> sorted.map { it - sorted.first() } }

    private fun constant(frames: Int, bFrames: Int = 0) = VideoSpec(frames, { it * 33_333L }, bFrames)

    private fun irregular(frames: Int) = VideoSpec(frames, { i -> (0 until i).sumOf { if (it % 3 == 0) 100_000L else 33_333L } })

    @Test
    fun constantRateVideoIsIndexedCompletely() = runBlocking {
        val spec = constant(90)
        val file = generate(spec)
        val truth = GeneratedVideo.sampleTimes(file)
        assumeTrue(truth.size == spec.frames)
        val (id, pts) = index(file)
        assertEquals(relative(truth), relative(pts))
        assertEquals(spec.frames, db.videoIndexDao().count(id))
        assertTrue(db.videoIndexDao().bySampleIndex(id, 0)!!.isKeyframe)
    }

    @Test
    fun variableRateVideoKeepsIrregularTimestamps() = runBlocking {
        val spec = irregular(90)
        val file = generate(spec)
        val truth = GeneratedVideo.sampleTimes(file)
        assumeTrue(truth.size == spec.frames)
        val (_, pts) = index(file)
        assertEquals(relative(truth), relative(pts))
        val expected = (0 until spec.frames).map { spec.ptsUs(it) }
        relative(expected).zip(relative(pts)).forEach { (want, got) -> assertTrue("pts drift $want vs $got", kotlin.math.abs(want - got) <= MUXER_ROUNDING_US) }
        assertTrue(pts.zipWithNext { a, b -> b - a }.toSet().size > 1)
        assertEquals(pts.sorted(), pts)
    }

    @Test
    fun bFrameVideoIsIndexedInDisplayOrder() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= 29)
        val spec = constant(90, bFrames = 2)
        val file = generate(spec)
        val truth = GeneratedVideo.sampleTimes(file)
        assumeTrue(truth.size == spec.frames)
        assumeTrue(GeneratedVideo.isReordered(truth))
        val (id, pts) = index(file)
        assertEquals(truth.sorted(), pts)
        val entries = db.videoIndexDao().list(id)
        assertTrue(entries.all { it.nearestKeyframeIndex <= it.sampleIndex })
    }

    @Test
    fun systemDecoderExtractsEveryFrame() = runBlocking {
        val spec = irregular(60)
        val file = generate(spec)
        val truth = GeneratedVideo.sampleTimes(file)
        assumeTrue(truth.size == spec.frames)
        val id = project(file)
        val engine = ExtractEngine(context, db.projectDao(), db.frameDao(), storage, db.videoIndexDao(), CheckpointStore(db))
        engine.run(ExtractRequest(id, Uri.fromFile(file).toString(), 0L, 0L, ExtractMode.EveryFrame, false))
        val last = engine.progress.value
        assertEquals("status=${last.status} message=${last.message}\n${if (db.frameDao().countAll(id) != spec.frames) recentLog() else ""}", spec.frames, db.frameDao().countAll(id))
        assertEquals(relative(truth), relative(db.frameDao().listPts(id)))
    }

    @Test
    fun cancelledExtractionResumesWithoutDuplicates() = runBlocking {
        val spec = constant(90)
        val file = generate(spec)
        val truth = GeneratedVideo.sampleTimes(file)
        assumeTrue(truth.size == spec.frames)
        val id = project(file)
        val request = ExtractRequest(id, Uri.fromFile(file).toString(), 0L, 0L, ExtractMode.EveryFrame, false)
        val first = ExtractEngine(context, db.projectDao(), db.frameDao(), storage, db.videoIndexDao(), CheckpointStore(db))
        val watcher = launch(Dispatchers.Default) {
            first.progress.collect { if (it.projectId == id && it.status == ExtractProgress.Status.Extracting && it.done >= 3) first.cancel() }
        }
        runCatching { first.run(request) }
        watcher.cancelAndJoin()
        val partial = db.frameDao().countAll(id)
        assertTrue("partial $partial status=${first.progress.value.status} message=${first.progress.value.message}\n${recentLog()}", partial in 1 until spec.frames)
        ExtractEngine(context, db.projectDao(), db.frameDao(), storage, db.videoIndexDao(), CheckpointStore(db)).run(request)
        val pts = db.frameDao().listPts(id)
        assertEquals("resumed ${pts.size} of ${spec.frames}\n${recentLog()}", spec.frames, pts.size)
        assertEquals(pts.size, pts.toSet().size)
        assertEquals(relative(truth), relative(pts))
    }
}
