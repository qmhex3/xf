package com.frameout.app.frames

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.thumb.FocusedImage
import com.frameout.app.thumb.ThumbPlaceholder
import com.frameout.app.thumb.rememberThumb
import com.frameout.core.db.AlbumCover
import com.frameout.core.db.CoverLane
import com.frameout.core.db.FrameEntity
import com.frameout.core.quality.CoverLanes
import com.frameout.core.quality.NormBox
import com.frameout.core.quality.TagNames
import com.frameout.core.quality.TagRules
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ThumbCache
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import kotlinx.coroutines.delay

private const val BEAT_MS = 1500L
private const val MAIN_PERIOD = 3
private const val TILE_PERIOD = 4
private const val TILE_SIZE = 256
private const val MAIN_SIZE = 384
private const val FOCUS_SIZE = 512
private const val ZOOM_MS = 5200
private const val MAX_COLUMNS_WITH_STRIP = 3
private const val MAX_COLUMNS_WITH_NAMES = 2
private val FILM_BLACK = Color(0xFF0B0B0B)

@Composable
internal fun AlbumCoverCard(label: String, count: Int, tag: String, cover: suspend (String) -> AlbumCover?, columns: Int, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val arabic = LocalConfiguration.current.locales[0].language == "ar"
    val calm = LocalSystemA11y.current.reduceMotion
    val albumCover = produceState<AlbumCover?>(null, tag) { value = cover(tag) }.value
    val lanes = albumCover?.lanes.orEmpty()
    val sequence = remember(albumCover) {
        CoverLanes.sequence(lanes).ifEmpty { albumCover?.let { listOf(0 to it.frame) }.orEmpty() }
    }
    val animated = !calm && sequence.size > 1
    var tick by remember(tag) { mutableIntStateOf(0) }
    LaunchedEffect(tag, animated) {
        if (!animated) return@LaunchedEffect
        delay(((tag.hashCode() and 0x7fffffff) % 1200).toLong())
        while (true) {
            delay(BEAT_MS)
            tick++
        }
    }
    val mainIndex = if (sequence.isEmpty()) 0 else steps(tick, MAIN_PERIOD, MAIN_PERIOD) % sequence.size
    val current = sequence.getOrNull(mainIndex)
    val showStrip = lanes.size >= 2 && columns <= MAX_COLUMNS_WITH_STRIP
    val activeLane = if (lanes.size >= 2) current?.first ?: -1 else -1
    val kindName = lanes.getOrNull(activeLane)?.kind?.let { TagNames.display(TagRules.LABEL + it, arabic) }
    val nextMain = sequence.getOrNull((mainIndex + 1) % sequence.size.coerceAtLeast(1))?.second
    val mainSize = if (columns <= 2) MAIN_SIZE else TILE_SIZE
    Prefetch(nextMain, mainSize)

    Box(
        Modifier
            .aspectRatio(1f)
            .clip(FrameShapes.xl)
            .background(colors.container)
            .border(1.dp, frameHairline(), FrameShapes.xl)
            .frameClickable(onClick = onClick)
            .semantics { contentDescription = "$label, ${FrameStrings.frameCount(count)}" }
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(if (showStrip) 0.66f else 1f)) {
                AnimatedContent(
                    targetState = current?.second,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        (fadeIn(tween(700)) + slideInVertically(tween(900, easing = FastOutSlowInEasing)) { it / 14 }) togetherWith fadeOut(tween(700))
                    },
                    contentKey = { it?.id },
                    label = "albumMain"
                ) { frame ->
                    val focus = if (frame != null && frame.id == albumCover?.frame?.id) albumCover?.focus else null
                    ReelImage(frame, focus, mainSize, animated, Modifier.fillMaxSize())
                }
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
                ) {
                    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 8.dp)) {
                        FrameText(label, style = FrameTheme.typography.label, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        FrameText(FrameStrings.frameCount(count), style = FrameTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
                    }
                }
                KindPill(kindName, Modifier.align(Alignment.TopStart).padding(8.dp))
            }
            if (showStrip) {
                FilmStrip(lanes, tick, activeLane, columns <= MAX_COLUMNS_WITH_NAMES, arabic, Modifier.fillMaxWidth().weight(0.34f))
            }
        }
    }
}

@Composable
private fun KindPill(name: String?, modifier: Modifier) {
    AnimatedContent(
        targetState = name,
        modifier = modifier,
        transitionSpec = { (fadeIn(tween(350, delayMillis = 120)) + slideInVertically(tween(350)) { -it / 2 }) togetherWith fadeOut(tween(200)) },
        label = "albumKind"
    ) { text ->
        if (text != null) {
            Box(Modifier.clip(FrameShapes.pill).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                FrameText(text, style = FrameTheme.typography.bodySmall, color = Color.White, maxLines = 1)
            }
        }
    }
}

@Composable
private fun FilmStrip(lanes: List<CoverLane>, tick: Int, active: Int, showNames: Boolean, arabic: Boolean, modifier: Modifier) {
    val holes = Color.White.copy(alpha = 0.34f)
    Box(modifier.background(FILM_BLACK).drawBehind { drawPerforations(holes) }) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            lanes.forEachIndexed { slot, lane ->
                FilmTile(lane, tick, slot, slot == active, showNames, arabic, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun FilmTile(lane: CoverLane, tick: Int, slot: Int, active: Boolean, showName: Boolean, arabic: Boolean, modifier: Modifier) {
    val frames = lane.frames
    val index = if (frames.isEmpty()) 0 else steps(tick, TILE_PERIOD, 1 + slot) % frames.size
    val frame = frames.getOrNull(index)
    Prefetch(frames.getOrNull((index + 1) % frames.size.coerceAtLeast(1)), TILE_SIZE)
    val ring by animateColorAsState(if (active) FrameTheme.colors.primary else Color.Transparent, tween(350), label = "tileRing")
    val lift by animateFloatAsState(if (active) 1f else 0.93f, spring(dampingRatio = 0.6f, stiffness = 220f), label = "tileLift")
    val shape = RoundedCornerShape(5.dp)
    Box(modifier.graphicsLayer { scaleX = lift; scaleY = lift }.clip(shape).border(1.5.dp, ring, shape)) {
        AnimatedContent(
            targetState = frame,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (slideInHorizontally(tween(520, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(520))) togetherWith
                    (slideOutHorizontally(tween(520, easing = FastOutSlowInEasing)) { -it / 3 } + fadeOut(tween(420)))
            },
            contentKey = { it?.id },
            label = "albumTile"
        ) { f -> ReelImage(f, null, TILE_SIZE, false, Modifier.fillMaxSize()) }
        val name = lane.kind
        if (showName && name != null) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                    .padding(top = 10.dp, bottom = 2.dp)
            ) {
                FrameText(
                    TagNames.display(TagRules.LABEL + name, arabic),
                    style = FrameTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReelImage(frame: FrameEntity?, focus: NormBox?, size: Int, zoom: Boolean, modifier: Modifier) {
    val path = frame?.let { it.thumbnailPath ?: it.fullPath }
    val px = if (focus != null) maxOf(size, FOCUS_SIZE) else size
    val bitmap = if (path == null) null else rememberThumb("$path#$px", { ThumbCache.peek(path, px) }, { ThumbCache.load(path, px) }).value
    val scale = remember { Animatable(1f) }
    if (zoom) LaunchedEffect(Unit) { scale.animateTo(1.08f, tween(ZOOM_MS, easing = LinearEasing)) }
    Box(modifier.background(FrameTheme.colors.container)) {
        if (bitmap != null && !bitmap.isRecycled) {
            val layer = Modifier.fillMaxSize().graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            if (focus != null) FocusedImage(bitmap, focus, layer)
            else Image(bitmap.asImageBitmap(), null, layer, contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize(), Alignment.Center) { ThumbPlaceholder() }
        }
    }
}

@Composable
private fun Prefetch(frame: FrameEntity?, size: Int) {
    val path = frame?.let { it.thumbnailPath ?: it.fullPath } ?: return
    LaunchedEffect(path, size) { ThumbCache.load(path, size) }
}

private fun steps(tick: Int, period: Int, offset: Int): Int = if (tick < offset) 0 else (tick - offset) / period + 1

private fun DrawScope.drawPerforations(color: Color) {
    val hole = Size(5.dp.toPx(), 3.dp.toPx())
    val gap = 5.dp.toPx()
    val edge = 2.dp.toPx()
    val radius = CornerRadius(1.dp.toPx())
    var x = 4.dp.toPx()
    while (x + hole.width < size.width) {
        drawRoundRect(color, Offset(x, edge), hole, radius)
        drawRoundRect(color, Offset(x, size.height - edge - hole.height), hole, radius)
        x += hole.width + gap
    }
}
