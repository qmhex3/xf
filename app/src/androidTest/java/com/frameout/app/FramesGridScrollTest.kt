package com.frameout.app

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.app.frames.FrameCell
import com.frameout.app.frames.FramePlaceholder
import com.frameout.app.frames.GridFit
import com.frameout.core.db.AppDatabase
import com.frameout.core.db.FrameEntity
import com.frameout.core.db.ProjectEntity
import com.frameout.core.storage.StorageManager
import com.frameout.designsystem.theme.FrameTheme
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private class ScrollResult(val deltasNs: List<Long>, val reachedEnd: Boolean, val periodNs: Long)

@RunWith(AndroidJUnit4::class)
class FramesGridScrollTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val maxJankPercent = InstrumentationRegistry.getArguments().getString("maxJankPercent")?.toDoubleOrNull() ?: 5.0
    private var projectId = 0L

    @Before
    fun seed() = runBlocking {
        val db = AppDatabase.get(context)
        val now = System.currentTimeMillis()
        projectId = db.projectDao().insert(ProjectEntity(name = "grid", sourceUri = "", sourceName = "grid.mp4", createdAt = now, updatedAt = now))
        val dir = StorageManager(context).thumbsDir(projectId)
        val paths = (0 until DISTINCT_THUMBS).map { writeThumb(dir, it) }
        (0 until FRAMES).chunked(500).forEach { chunk ->
            db.frameDao().insertAll(chunk.map { FrameEntity(projectId = projectId, index = it, ptsUs = it * 16_667L, thumbnailPath = paths[it % paths.size]) })
        }
    }

    @After
    fun cleanup() {
        runBlocking {
            AppDatabase.get(context).projectDao().delete(projectId)
            StorageManager(context).deleteProject(projectId)
        }
    }

    private fun writeThumb(dir: File, i: Int): String {
        val file = File(dir, "g_$i.jpg")
        val bitmap = Bitmap.createBitmap(THUMB_SIDE, THUMB_SIDE, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(i * 13 % 256, i * 7 % 256, i * 29 % 256))
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        bitmap.recycle()
        return file.absolutePath
    }

    @Test
    fun scrollsThousandsOfFramesWithoutDroppingBelowSixtyFps() {
        assumeFalse("frame timing is meaningless on software-rendered emulators", isEmulator())
        val done = CountDownLatch(1)
        val result = AtomicReference<ScrollResult>()
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent { FrameTheme { ScrollHarness(projectId) { result.set(it); done.countDown() } } }
            }
            assertTrue("scroll did not finish", done.await(240, TimeUnit.SECONDS))
        }
        val r = result.get()
        assertTrue("grid did not reach the last frame", r.reachedEnd)
        assertTrue("too few frames measured: ${r.deltasNs.size}", r.deltasNs.size >= 300)
        val missed = r.deltasNs.count { it > r.periodNs * 3 / 2 }
        val percent = missed * 100.0 / r.deltasNs.size
        assertTrue("missed $percent% of ${r.deltasNs.size} frames", percent <= maxJankPercent)
        assertTrue("average frame ${r.deltasNs.average() / 1e6}ms", r.deltasNs.average() <= r.periodNs * 1.2)
    }

    private fun isEmulator() = Build.FINGERPRINT.contains("generic") || Build.FINGERPRINT.contains("emulator") || Build.MODEL.contains("Emulator") || Build.MODEL.contains("sdk_gphone") || Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish")

    private companion object {
        const val FRAMES = 5_000
        const val DISTINCT_THUMBS = 1_200
        const val THUMB_SIDE = 256
    }
}

@Composable
private fun ScrollHarness(projectId: Long, onDone: (ScrollResult) -> Unit) {
    val dao = AppDatabase.get(LocalContext.current).frameDao()
    val pages = remember { Pager(PagingConfig(60, prefetchDistance = 30, enablePlaceholders = true)) { dao.pagingActive(projectId) }.flow }.collectAsLazyPagingItems()
    val state = rememberLazyGridState()
    val view = LocalView.current
    LazyVerticalGrid(state = state, columns = GridCells.Fixed(8), modifier = Modifier.fillMaxSize()) {
        items(pages.itemCount, key = { i -> pages.peek(i)?.id ?: i }) { i ->
            val frame = pages[i]
            if (frame == null) FramePlaceholder() else FrameCell(frame, false, null, GridFit.Fill, {}, {})
        }
    }
    LaunchedEffect(pages.itemCount) {
        if (pages.itemCount < 5_000) return@LaunchedEffect
        val refresh = view.display?.refreshRate?.takeIf { it > 0f } ?: 60f
        onDone(measureScroll(state, (1_000_000_000.0 / refresh).toLong()))
    }
}

private suspend fun measureScroll(state: LazyGridState, periodNs: Long): ScrollResult {
    repeat(30) { withFrameNanos { }; state.scrollBy(60f) }
    val deltas = ArrayList<Long>()
    var last = withFrameNanos { it }
    var frames = 0
    while (state.canScrollForward && frames < 4_000) {
        val now = withFrameNanos { it }
        deltas += now - last
        last = now
        state.scrollBy(60f)
        frames++
    }
    return ScrollResult(deltas, !state.canScrollForward, periodNs)
}
