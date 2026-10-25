package com.frameout.app.library

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import com.frameout.app.viewer.PagerPage
import com.frameout.app.viewer.viewerFling
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.frameout.app.FrameOutApp
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.asChooserWithoutSelf
import com.frameout.app.viewer.SavedPlaylist
import com.frameout.app.viewer.detectViewportGestures
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.logFailure
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.components.TopBarIconButton
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameColors
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_ZOOM = 6f
private const val DOUBLE_TAP_ZOOM = 3f
private const val ZOOMED = 1.05f

@Composable
fun SavedImageViewerScreen(savedId: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FrameOutApp
    val ids = remember(savedId) { SavedPlaylist.ids.takeIf { savedId in it } ?: listOf(savedId) }
    val pager = rememberPagerState(initialPage = ids.indexOf(savedId).coerceAtLeast(0)) { ids.size }
    var chrome by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<Map<Long, com.frameout.core.db.SavedImageEntity?>>(emptyMap()) }
    LaunchedEffect(pager.settledPage) { zoomed = false }
    LaunchedEffect(pager.currentPage) {
        val id = ids[pager.currentPage]
        if (id !in items) {
            val entity = withContext(Dispatchers.IO) { runCatching { app.container.savedImages.getById(id) }.getOrNull() }
            items = items + (id to entity)
        }
    }
    BackHandler { onClose() }

    PreferredFrameTheme {
        Box(Modifier.fillMaxSize().background(FrameColors.ViewerBlack)) {
            SavedImagePager(pager, ids, !zoomed, onZoomed = { zoomed = it }, onTap = { chrome = !chrome })
            val label = if (ids.size > 1) "${pager.currentPage + 1} / ${ids.size}" else items[ids[pager.currentPage]]?.displayName.orEmpty()
            AnimatedVisibility(
                visible = chrome,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                SavedImageTopBar(label, items[ids[pager.currentPage]]?.uri, onClose, Modifier)
            }
        }
    }
}

@Composable
private fun SavedImagePager(pager: PagerState, ids: List<Long>, scrollEnabled: Boolean, onZoomed: (Boolean) -> Unit, onTap: () -> Unit) {
    HorizontalPager(
        state = pager,
        userScrollEnabled = scrollEnabled,
        beyondViewportPageCount = 1,
        flingBehavior = viewerFling(pager),
        modifier = Modifier.fillMaxSize()
    ) { page ->
        PagerPage(pager, page) {
            SavedImagePage(
                id = ids[page],
                active = page == pager.currentPage,
                onZoomed = { if (page == pager.currentPage) onZoomed(it) },
                onTap = onTap
            )
        }
    }
}

@Composable
private fun SavedImageTopBar(label: String, shareUri: String?, onClose: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    Box(
        modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.45f))
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        TopBarIconButton(FrameIcons.Back, FrameStrings.back(), onClose, Modifier.align(Alignment.CenterStart))
        FrameText(label, color = Color.White, maxLines = 1, modifier = Modifier.align(Alignment.Center))
        if (shareUri != null) {
            TopBarIconButton(FrameIcons.Share, FrameStrings.share(), { share(context, shareUri) }, Modifier.align(Alignment.CenterEnd))
        }
    }
}

private class ZoomState {
    var scale by mutableStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    var viewSize by mutableStateOf(IntSize.Zero)
    val zoomed: Boolean get() = scale > ZOOMED

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    private fun clamp(o: Offset, s: Float): Offset {
        val maxX = viewSize.width * (s - 1f) / 2f
        val maxY = viewSize.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    fun doubleTap(tap: Offset, size: IntSize) {
        if (zoomed) {
            reset()
        } else {
            val center = Offset(size.width / 2f, size.height / 2f)
            scale = DOUBLE_TAP_ZOOM
            offset = clamp((center - tap) * (DOUBLE_TAP_ZOOM - 1f), DOUBLE_TAP_ZOOM)
        }
    }

    fun transform(centroid: Offset, pan: Offset, zoom: Float, size: IntSize) {
        val next = (scale * zoom).coerceIn(1f, MAX_ZOOM)
        val center = Offset(size.width / 2f, size.height / 2f)
        val anchored = (offset + pan) + (centroid - center - offset) * (1f - next / scale)
        scale = next
        offset = if (next <= 1f) Offset.Zero else clamp(anchored, next)
    }
}

@Composable
private fun rememberLoaded(id: Long, viewSize: IntSize): Loaded {
    val context = LocalContext.current
    val app = context.applicationContext as FrameOutApp
    val loaded by produceState<Loaded>(Loaded.Loading, id, viewSize) {
        if (viewSize == IntSize.Zero) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val entity = app.container.savedImages.getById(id) ?: return@runCatching Loaded.Missing
                val uri = Uri.parse(entity.uri)
                val side = maxOf(viewSize.width, viewSize.height).coerceAtLeast(1)
                val bitmap = SavedMediaDecoder(context).decode(uri, entity.displayName, side)
                if (bitmap == null) Loaded.Missing else Loaded.Ready(bitmap)
            }.onFailure { logFailure("SavedImageViewer", it) }.getOrDefault(Loaded.Missing)
        }
    }
    return loaded
}

@Composable
private fun SavedImagePage(id: Long, active: Boolean, onZoomed: (Boolean) -> Unit, onTap: () -> Unit) {
    val zoom = remember { ZoomState() }
    val loaded = rememberLoaded(id, zoom.viewSize)
    LaunchedEffect(active) { if (!active) zoom.reset() }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { zoom.viewSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tap ->
                        zoom.doubleTap(tap, size)
                        onZoomed(zoom.zoomed)
                    }
                )
            }
            .pointerInput(Unit) {
                detectViewportGestures(singleFinger = { zoom.zoomed }) { centroid, pan, factor ->
                    zoom.transform(centroid, pan, factor, size)
                    onZoomed(zoom.zoomed)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        SavedImageContent(loaded, zoom)
    }
}

@Composable
private fun SavedImageContent(state: Loaded, zoom: ZoomState) {
    when (state) {
        is Loaded.Ready -> Image(
            bitmap = remember(state.bitmap) { state.bitmap.asImageBitmap() },
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = if (zoom.scale >= 2f) FilterQuality.None else FilterQuality.Medium,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = zoom.scale
                scaleY = zoom.scale
                translationX = zoom.offset.x
                translationY = zoom.offset.y
            }
        )
        Loaded.Missing -> FrameText(FrameStrings.savedMissing(), color = Color.White.copy(alpha = 0.8f), style = FrameTheme.typography.body)
        Loaded.Loading -> Unit
    }
}

private sealed interface Loaded {
    data object Loading : Loaded
    data object Missing : Loaded
    class Ready(val bitmap: Bitmap) : Loaded
}

private fun share(context: Context, uriText: String) {
    val uri = Uri.parse(uriText)
    val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull() ?: "image/*"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(send.asChooserWithoutSelf(context, FrameStrings.share())) }
}
