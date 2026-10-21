package com.frameout.app.collage

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import com.frameout.app.FrameOutApp
import com.frameout.app.trim.SavedCapture
import com.frameout.core.collage.COLLAGE_CORNER_REFERENCE
import com.frameout.core.collage.COLLAGE_DEFAULT_WIDTH
import com.frameout.core.collage.CellCrop
import com.frameout.core.collage.CollageAspect
import com.frameout.core.collage.CollageTemplate
import com.frameout.core.collage.CollageTemplates
import com.frameout.core.db.FrameEntity
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.ThumbCache
import com.frameout.engine.timeline.formatTimestamp


@Stable
class CollageState(frameCount: Int, prefs: AppPrefs) {
    var frames by mutableStateOf<List<FrameEntity>>(emptyList())
    var paths by mutableStateOf<List<String?>>(emptyList())
    var previews by mutableStateOf<List<Bitmap?>>(emptyList())
    var order by mutableStateOf<List<Int>>(emptyList())
    var ready by mutableStateOf(false)
    var template by mutableStateOf(CollageTemplates.suggest(frameCount))
    var aspect by mutableStateOf(CollageAspect.Template)
    var resolution by mutableIntStateOf(COLLAGE_DEFAULT_WIDTH)
    var crops by mutableStateOf<List<CellCrop>>(emptyList())
    var gap by mutableIntStateOf(prefs.collageGap.value)
    var bgColor by mutableIntStateOf(prefs.collageBgColor.value)
    var pickerOpen by mutableStateOf(false)
    var rounded by mutableStateOf(prefs.collageRounded.value)
    var stamps by mutableStateOf(prefs.collageStamps.value)
    var selectedCell by mutableStateOf<Int?>(null)
    var saving by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var messageIsError by mutableStateOf(false)
    var result by mutableStateOf<SavedCapture?>(null)
    private var userPicked = false
    var saveJob: kotlinx.coroutines.Job? = null
    private var leaveArmed = false

    fun holdBack(): Boolean {
        if (!saving) return false
        if (leaveArmed) {
            saveJob?.cancel()
            return false
        }
        leaveArmed = true
        messageIsError = false
        message = com.frameout.core.util.ClipStrings.saveWait()
        return true
    }

    fun saveEnded() {
        saving = false
        saveJob = null
        leaveArmed = false
    }

    val canvasTemplate: CollageTemplate get() = template.withAspect(aspect)
    val background: Color get() = Color(bgColor)
    val style: PreviewStyle get() = PreviewStyle(gap, if (rounded) COLLAGE_CORNER_REFERENCE else 0f, background, stamps)

    fun pickTemplate(t: CollageTemplate) {
        template = t
        userPicked = true
        selectedCell = null
        crops = crops.map { CellCrop() }
    }

    fun cropOf(frameIndex: Int): CellCrop = crops.getOrNull(frameIndex) ?: CellCrop()

    fun setCrop(cell: Int, crop: CellCrop) {
        val frameIndex = order.getOrNull(cell) ?: return
        if (frameIndex !in crops.indices) return
        crops = crops.toMutableList().also { it[frameIndex] = crop }
    }

    fun tapCell(i: Int) {
        val filled = i < order.size && i < template.cells.size
        val sel = selectedCell
        selectedCell = when {
            !filled -> null
            sel == null -> i
            sel == i -> null
            else -> {
                swap(sel, i)
                null
            }
        }
    }

    private fun swap(a: Int, b: Int) {
        val first = order[a]
        val second = order[b]
        order = order.toMutableList().also { it[a] = second; it[b] = first }
        crops = crops.toMutableList().also {
            if (first in it.indices) it[first] = CellCrop()
            if (second in it.indices) it[second] = CellCrop()
        }
    }

    fun slots(): List<PreviewSlot?> = template.cells.indices.map { i ->
        val idx = order.getOrNull(i) ?: return@map null
        val frame = frames.getOrNull(idx) ?: return@map null
        PreviewSlot(previews.getOrNull(idx), formatTimestamp(frame.ptsUs / 1000L), i + 1, cropOf(idx))
    }

    fun cropsInOrder(): List<CellCrop> = order.map { cropOf(it) }

    fun stampsInOrder(): List<String> = order.map { formatTimestamp((frames.getOrNull(it)?.ptsUs ?: 0L) / 1000L) }

    suspend fun load(context: Context, app: FrameOutApp, ids: List<Long>) {
        ready = false
        val loaded = ids.mapNotNull { app.container.frames.getById(it) }
        frames = loaded
        order = loaded.indices.toList()
        crops = loaded.map { CellCrop() }
        if (!userPicked) template = CollageTemplates.suggest(loaded.size)
        previews = loaded.map { f -> ThumbCache.load(f.thumbnailPath ?: f.fullPath, 512) }
        val materialized = CollageMaterializer(context, app.container.projects, app.container.frameEdits).materialize(loaded)
        val usable = loaded.indices.filter { materialized[it] != null }
        if (usable.size != loaded.size) {
            val thumbs = previews
            frames = usable.map { loaded[it] }
            previews = usable.map { thumbs.getOrNull(it) }
            order = frames.indices.toList()
            crops = frames.map { CellCrop() }
            selectedCell = null
            if (!userPicked) template = CollageTemplates.suggest(frames.size)
        }
        paths = usable.map { materialized[it] }
        ready = true
        previews = paths.mapIndexed { i, p -> loadPreview(p) ?: previews.getOrNull(i) }
        if (frames.size != ids.size) {
            messageIsError = true
            message = com.frameout.core.util.CollageStrings.skipped(ids.size - frames.size, ids.size)
        }
    }
}

private data class CollageOptions(val gap: Int, val bgColor: Int, val rounded: Boolean, val stamps: Boolean)

suspend fun CollageState.persistOptions(prefs: AppPrefs) {
    snapshotFlow { CollageOptions(gap, bgColor, rounded, stamps) }.collect {
        prefs.setCollageGap(it.gap)
        prefs.setCollageBgColor(it.bgColor)
        prefs.setCollageRounded(it.rounded)
        prefs.setCollageStamps(it.stamps)
    }
}
