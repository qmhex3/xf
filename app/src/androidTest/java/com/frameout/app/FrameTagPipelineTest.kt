package com.frameout.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.core.db.AppDatabase
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectEntity
import com.frameout.core.quality.FrameTagScanner
import com.frameout.core.quality.FrameTagger
import com.frameout.core.quality.ScanVerdict
import com.frameout.core.storage.StorageManager
import com.frameout.core.util.ThumbCache
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FrameTagPipelineTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = AppDatabase.get(context)
    private val storage = StorageManager(context)
    private val files = mutableListOf<File>()
    private val projects = mutableListOf<Long>()

    @After
    fun cleanup() = runBlocking {
        projects.forEach {
            FrameTagScanner.reset(db.frameTagDao(), it)
            db.projectDao().delete(it)
            storage.deleteProject(it)
        }
        files.forEach { it.delete() }
    }

    private fun scene(width: Int = 640, height: Int = 480): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(40, 90, 160))
        val paint = Paint().apply { color = Color.WHITE; textSize = 64f; isAntiAlias = true }
        canvas.drawText("FRAMEOUT 2026", 40f, 200f, paint)
        canvas.drawRect(300f, 260f, 560f, 420f, Paint().apply { color = Color.rgb(220, 160, 40) })
        return bitmap
    }

    private fun thumbnail(name: String, bytes: ByteArray? = null): File {
        val file = File(context.cacheDir, "tag_${System.nanoTime()}_$name.png").also { files += it }
        if (bytes != null) {
            file.writeBytes(bytes)
        } else {
            FileOutputStream(file).use { scene().compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return file
    }

    private suspend fun project(thumbs: List<File>): Long {
        val now = System.currentTimeMillis()
        val id = db.projectDao().insert(ProjectEntity(name = "tag", sourceUri = "file:///none", sourceName = "none", createdAt = now, updatedAt = now))
        projects += id
        db.frameDao().insertAll(thumbs.mapIndexed { i, f -> FrameEntity(projectId = id, index = i, ptsUs = i * 33_333L, thumbnailPath = f.absolutePath) })
        return id
    }

    @Test
    fun detectorsRunOnARealBitmapWithoutAllFailing() = runBlocking {
        val outcome = FrameTagger.tagDetailed("pipeline-${System.nanoTime()}", scene())
        assertTrue("failed detectors: ${outcome.failed}", outcome.failed.size < FrameTagger.DETECTORS)
    }

    @Test
    fun corruptThumbnailsAreReportedAsFailedAndStayPending() = runBlocking {
        val id = project(List(4) { thumbnail("bad$it", ByteArray(64) { b -> b.toByte() }) })
        val report = FrameTagScanner.scan(db.frameTagDao(), id) { }
        assertEquals(ScanVerdict.Failed, report.verdict)
        assertEquals(4, report.failed)
        assertEquals(0, report.analyzed)
        assertEquals(4, db.frameTagDao().countUnscanned(id))
    }

    @Test
    fun readableThumbnailsAreAnalyzedAndMarkedScanned() = runBlocking {
        val id = project(List(3) { thumbnail("ok$it") })
        val report = FrameTagScanner.scan(db.frameTagDao(), id) { }
        assertEquals("report: ${report.verdict} ${report.detectors}", 0, report.failed)
        assertEquals(3, report.analyzed + report.partial)
        assertTrue(db.frameTagDao().countUnscanned(id) <= report.partial)
    }

    @Test
    fun pooledDecodeReusesTheReleasedBitmap() {
        val first = ThumbCache.decodePooled(thumbnail("a").absolutePath, 480)
        assertNotNull(first)
        ThumbCache.release(first)
        val second = ThumbCache.decodePooled(thumbnail("b").absolutePath, 480)
        assertSame(first, second)
        ThumbCache.release(second)
    }
}
