package com.frameout.app.collage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.FrameOutApp
import com.frameout.app.settings.BottomSheetFrame
import com.frameout.app.thumb.rememberThumb
import com.frameout.core.db.FrameEntity
import com.frameout.core.util.CollageStrings
import com.frameout.core.util.ThumbCache
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameSpinner
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.timeline.formatTimestamp
import kotlinx.coroutines.launch

private const val PAGE = 60

@Composable
internal fun FramePickerSheet(state: CollageState, projectId: Long, app: FrameOutApp) {
    val visible = state.pickerSlot != null
    BottomSheetFrame(visible, { state.pickerSlot = null }, CollageStrings.pickFrame()) {
        if (visible) PickerGrid(state, projectId, app)
    }
}

@Composable
private fun ColumnScope.PickerGrid(state: CollageState, projectId: Long, app: FrameOutApp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loaded = remember { mutableStateListOf<FrameEntity>() }
    var limit by remember { mutableIntStateOf(PAGE) }
    var done by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    LaunchedEffect(limit) {
        val page = app.container.frames.pageActive(projectId, PAGE, limit - PAGE)
        loaded.addAll(page)
        if (page.size < PAGE) done = true
    }
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 12
        }
    }
    LaunchedEffect(nearEnd, done, loaded.size) {
        if (nearEnd && !done && loaded.size >= limit) limit += PAGE
    }
    val usedIds = state.frames.map { it.id }.toSet()
    val shown = loaded.filter { it.id !in usedIds }
    if (done && shown.isEmpty()) {
        FrameText(
            CollageStrings.noFrames(),
            style = FrameTheme.typography.bodySmall,
            color = FrameTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        )
        return
    }
    if (!done && loaded.isEmpty()) FrameSpinner(Modifier.align(Alignment.CenterHorizontally).padding(24.dp), size = 28.dp)
    if (adding) FrameSpinner(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp), size = 22.dp)
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(shown, key = { it.id }) { frame ->
            PickCell(frame, !adding) {
                adding = true
                scope.launch {
                    val ok = state.addFrame(context, app, frame)
                    adding = false
                    if (ok) {
                        state.pickerSlot = null
                    } else {
                        state.messageIsError = true
                        state.message = CollageStrings.skipped(1, 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickCell(frame: FrameEntity, enabled: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val path = frame.thumbnailPath ?: frame.fullPath
    val bitmap = rememberThumb(path, { ThumbCache.peek(path) }, { ThumbCache.load(path, 256) }).value
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(FrameShapes.sm)
            .background(colors.containerHigh)
            .frameClickable(enabled = enabled, onClick = onClick)
    ) {
        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop) }
        FrameText(
            formatTimestamp(frame.ptsUs / 1000L),
            style = FrameTheme.typography.monoSmall.copy(fontSize = 9.sp),
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).background(Color.Black.copy(alpha = 0.6f), FrameShapes.xs).padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}
