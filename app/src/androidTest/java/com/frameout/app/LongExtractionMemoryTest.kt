package com.frameout.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Debug
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
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LongExtractionMemoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val frames = InstrumentationRegistry.getArguments().getString("longFrames")?.toIntOrNull() ?: 6_000
    private val maxGrowthBytes = (InstrumentationRegistry.getArguments().getString("maxGrowthMb")?.toLongOrNull() ?: 48L) * MB
    private val peakBudgetBytes = 256L * MB

    private class Sample(val done: Int, val bytes: Long)

    private fun usedBytes(): Long {
        repeat(2) { System.gc() }
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory() + Debug.getNativeHeapAllocatedSize()
    }

    private fun frameSpec(i: Int): ApngFrameSpec = ApngFrameSpec(0, 0, 0, 0, 1, 60) {
        Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(i * 7 % 256, i * 3 % 256, 128))
            val paint = Paint().apply { color = Color.rgb(255 - i % 256, i * 11 % 256, i * 5 % 256) }
            val left = (i * 5 % (WIDTH - 60)).toFloat()
            canvas.drawRect(left, 40f, left + 60f, 100f, paint)
        }
    }

    @Test
    fun longSequenceExtractsWithFlatMemory() = runBlocking {
        val file = File(context.cacheDir, "long_${System.nanoTime()}.apng")
        val db = AppDatabase.get(context)
        val storage = StorageManager(context)
        var projectId = 0L
        try {
            ApngTestWriter.write(file, WIDTH, HEIGHT, frames) { frameSpec(it) }
            val uri = Uri.fromFile(file).toString()
            val now = System.currentTimeMillis()
            projectId = db.projectDao().insert(ProjectEntity(name = "long", sourceUri = uri, sourceName = file.name, createdAt = now, updatedAt = now))
            val engine = ExtractEngine(context, db.projectDao(), db.frameDao(), storage, db.videoIndexDao(), CheckpointStore(db))
            val samples = CopyOnWriteArrayList<Sample>()
            val step = (frames / 20).coerceAtLeast(1)
            val watcher = launch(Dispatchers.Default) {
                var bucket = -1
                engine.progress.collect { p ->
                    if (p.projectId == projectId && p.status == ExtractProgress.Status.Extracting && p.done / step > bucket) {
                        bucket = p.done / step
                        samples += Sample(p.done, usedBytes())
                    }
                }
            }
            engine.run(ExtractRequest(projectId, uri, 0L, 0L, ExtractMode.EveryFrame, false))
            watcher.cancelAndJoin()
            assertEquals(frames, db.frameDao().countAll(projectId))
            assertTrue("samples ${samples.size}", samples.size >= 8)
            val warm = samples.filter { it.done in frames / 10..frames / 3 }.maxOf { it.bytes }
            val late = samples.filter { it.done >= frames * 2 / 3 }.maxOf { it.bytes }
            val peak = samples.maxOf { it.bytes }
            assertTrue("growth ${(late - warm) / MB}MB", late - warm <= maxGrowthBytes)
            assertTrue("peak ${peak / MB}MB", peak <= peakBudgetBytes)
        } finally {
            if (projectId != 0L) {
                db.projectDao().delete(projectId)
                storage.deleteProject(projectId)
            }
            file.delete()
        }
    }

    private companion object {
        const val MB = 1024L * 1024L
        const val WIDTH = 480
        const val HEIGHT = 270
    }
}
