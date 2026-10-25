package com.frameout.app.frames

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.frameout.designsystem.motion.fxBouncy
import com.frameout.designsystem.motion.fxPop
import com.frameout.designsystem.motion.fxPulse
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameCombinedClickable
import androidx.compose.ui.semantics.contentDescription
import com.frameout.core.util.FrameA11y
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import com.frameout.app.thumb.FocusedImage
import com.frameout.app.thumb.ThumbPlaceholder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.frameout.core.edit.DetectionMapper
import com.frameout.core.quality.NormBox
import com.frameout.core.util.ThumbCache
import com.frameout.app.thumb.rememberSharpThumb
import com.frameout.app.thumb.rememberThumb
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import com.frameout.app.CellOrigin
import androidx.compose.ui.unit.dp
import com.frameout.core.db.FrameEntity
import com.frameout.core.edit.FrameEdit
import com.frameout.core.quality.FaceAssessment
import com.frameout.core.quality.FaceLevel
import com.frameout.core.quality.FaceScanner
import com.frameout.core.util.EditedThumb
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.components.FrameText
import androidx.compose.animation.core.animateFloat
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import androidx.compose.ui.res.painterResource
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.engine.timeline.formatTimestamp

private const val FOCUS_THUMB = 512

@Composable
fun FrameCell(frame: FrameEntity, selected: Boolean, selectionIndex: Int?, fit: GridFit, onClick: () -> Unit, onLongClick: () -> Unit, onToggleFavorite: () -> Unit = {}, waitIdle: suspend () -> Unit = {}, edit: FrameEdit? = null, focus: NormBox? = null, modifier: Modifier = Modifier, hint: Boolean = false) {
    val colors = FrameTheme.colors
    val path = frame.thumbnailPath ?: frame.fullPath
    val editKey = EditedThumb.key(path, edit)
    val bitmap = rememberThumb(
        editKey ?: path,
        { EditedThumb.peek(path, edit) },
        { EditedThumb.load(path, edit) }
    ).value
    val faceKey = editKey ?: path
    val zoom = remember(focus, edit) { focus?.let { box -> if (edit == null) box else DetectionMapper.map(box, edit) } }
    val zoomSource = if (zoom != null && editKey == null) {
        rememberThumb("$path#focus", { ThumbCache.peek(path, FOCUS_THUMB) }, { ThumbCache.load(path, FOCUS_THUMB) }).value
    } else null
    val shown = zoomSource?.takeUnless { it.isRecycled } ?: bitmap
    var showWhy by remember { androidx.compose.runtime.mutableStateOf(false) }
    val face = produceState(FaceScanner.cached(faceKey) ?: FaceAssessment.NONE, bitmap) {
        val b = bitmap
        val cached = FaceScanner.cached(faceKey)
        value = when {
            cached != null -> cached
            b == null || b.isRecycled -> FaceAssessment.NONE
            else -> {
                delay(350)
                waitIdle()
                if (editKey == null) FaceScanner.assessFrame(frame) else FaceScanner.assess(faceKey, b)
            }
        }
    }.value
    val pulse = if (hint) {
        val p by androidx.compose.animation.core.rememberInfiniteTransition(label = "hintPulse").animateFloat(
            0.35f, 1f,
            androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(800), androidx.compose.animation.core.RepeatMode.Reverse),
            label = "hintAlpha"
        )
        p
    } else 1f
    val shrink by animateFloatAsState(if (selected) 0.9f else 1f, fxBouncy(), label = "cellShrink")
    val coords = remember { arrayOfNulls<androidx.compose.ui.layout.LayoutCoordinates>(1) }
    BoxWithConstraints(
        modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(colors.container, FrameShapes.sm)
            .then(if (selected) Modifier.border(2.dp, colors.primary, FrameShapes.sm) else Modifier)
            .then(if (hint) Modifier.border(3.dp, colors.primary.copy(alpha = pulse), FrameShapes.sm) else Modifier)
            .onGloballyPositioned { coords[0] = it }
            .frameCombinedClickable({ CellOrigin.mark(coords[0]); onClick() }, onLongClick, FrameA11y.select())
            .semantics {
                this.selected = selected
                contentDescription = FrameA11y.frame(frame.index, formatTimestamp(frame.ptsUs / 1000))
            }
    ) {
        val density = CellDensity.of(maxWidth)
        val cellPx = with(LocalDensity.current) { ((maxWidth.roundToPx() + 63) / 64 * 64).coerceIn(128, 1024) }
        val sharpPaths = remember(frame.fullPath, frame.thumbnailPath) { listOfNotNull(frame.fullPath, frame.thumbnailPath) }
        val sharp = rememberSharpThumb(
            "${sharpPaths.firstOrNull()}|$cellPx|${edit?.hashCode()}",
            { sharpPaths.firstNotNullOfOrNull { EditedThumb.peekSharp(it, edit, cellPx) } },
            { sharpPaths.firstNotNullOfOrNull { EditedThumb.loadSharp(it, edit, cellPx) } },
            waitIdle
        ).value
        val timeText = formatTimestamp(frame.ptsUs / 1000).let {
            if (density == CellDensity.Tiny) it.substringAfterLast(':') else it
        }
        Box(
            Modifier.fillMaxSize().graphicsLayer { scaleX = shrink; scaleY = shrink }.padding(if (fit == GridFit.Fit) density.fitPadding else 0.dp).background(colors.containerHigh, FrameShapes.xs),
            Alignment.Center
        ) {
            if (shown != null && zoom != null) {
                FocusedImage(shown, zoom, Modifier.fillMaxSize(), fit = fit == GridFit.Fit)
            } else if (bitmap != null) {
                val display = sharp?.takeUnless { it.isRecycled } ?: bitmap
                val image = remember(display) { display.asImageBitmap() }
                Image(
                    image,
                    null,
                    Modifier.fillMaxSize(),
                    contentScale = if (fit == GridFit.Fit) ContentScale.Fit else ContentScale.Crop,
                    filterQuality = if (display === sharp) FilterQuality.Medium else FilterQuality.Low
                )
            } else {
                ThumbPlaceholder()
            }
        }
        FrameText(
            timeText,
            FrameTheme.typography.label.copy(fontSize = density.timeSize, lineHeight = TextUnit.Unspecified),
            Color.White,
            Modifier
                .align(Alignment.BottomStart)
                .padding(density.edge)
                .widthIn(max = maxWidth - density.edge * 2)
                .background(Color.Black.copy(alpha = 0.55f), FrameShapes.xs)
                .padding(horizontal = density.timePadH, vertical = 1.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        Image(
            painterResource(if (frame.isFavorite) FrameIcons.StarFilled else FrameIcons.Star),
            FrameStrings.favorite(),
            Modifier
                .align(Alignment.TopStart)
                .fxPulse(frame.isFavorite)
                .padding(density.edge)
                .size(density.starSize)
                .background(Color.Black.copy(alpha = 0.4f), FrameShapes.pill)
                .padding(density.starInset)
                .frameClickable(onClick = onToggleFavorite),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                if (frame.isFavorite) colors.primary else Color.White.copy(alpha = 0.85f)
            )
        )
        selectionIndex?.let {
            Box(
                Modifier.align(Alignment.TopEnd).fxPop().padding(density.edge).defaultMinSize(density.badgeSize, density.badgeSize).background(colors.primary, FrameShapes.pill),
                Alignment.Center
            ) {
                FrameText("${it + 1}", FrameTheme.typography.label.copy(fontSize = density.timeSize, lineHeight = TextUnit.Unspecified), colors.onPrimary, Modifier.padding(horizontal = 2.dp), maxLines = 1)
            }
        }
        if (face.level != FaceLevel.None && !(density != CellDensity.Full && selectionIndex != null)) {
            FaceBadge(
                face,
                Modifier
                    .align(if (density == CellDensity.Full) Alignment.BottomEnd else Alignment.TopEnd)
                    .padding(density.edge),
                density.badgeSize,
                density.badgeIcon,
                onClick = { showWhy = true }
            )
        }
        if (showWhy) {
            FaceDetailsDialog(face) { showWhy = false }
        }
        if (hint) {
            androidx.compose.foundation.layout.Row(
                Modifier
                    .align(Alignment.Center)
                    .widthIn(max = maxWidth - 6.dp)
                    .background(colors.primary, FrameShapes.pill)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp)
            ) {
                Image(painterResource(FrameIconsExtra.GoToFrame), null, Modifier.size(12.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.onPrimary))
                FrameText(
                    FrameStrings.savedFrameHint(),
                    FrameTheme.typography.label.copy(fontSize = density.timeSize, lineHeight = TextUnit.Unspecified),
                    colors.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
        if (frame.isDeleted) {
            FrameText(
                FrameStrings.deleted(),
                FrameTheme.typography.label.copy(fontSize = density.timeSize, lineHeight = TextUnit.Unspecified),
                Color.White,
                Modifier
                    .align(Alignment.Center)
                    .background(colors.error.copy(alpha = 0.85f), FrameShapes.xs)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                maxLines = 1
            )
        }
    }
}

private enum class CellDensity(
    val edge: Dp, val timeSize: TextUnit, val timePadH: Dp,
    val starSize: Dp, val starInset: Dp, val badgeSize: Dp, val badgeIcon: Dp, val fitPadding: Dp
) {
    Full(4.dp, 11.sp, 4.dp, 22.dp, 3.dp, 20.dp, 14.dp, 8.dp),
    Medium(3.dp, 10.sp, 3.dp, 18.dp, 2.dp, 16.dp, 12.dp, 4.dp),
    Small(2.dp, 8.sp, 2.dp, 16.dp, 2.dp, 14.dp, 10.dp, 2.dp),
    Tiny(1.dp, 7.sp, 2.dp, 14.dp, 2.dp, 12.dp, 9.dp, 0.dp);

    companion object {
        fun of(width: Dp): CellDensity = when {
            width >= 120.dp -> Full
            width >= 84.dp -> Medium
            width >= 56.dp -> Small
            else -> Tiny
        }
    }
}

@Composable
fun FramePlaceholder() {
    Box(Modifier.aspectRatio(1f).padding(2.dp).background(FrameTheme.colors.container, FrameShapes.sm))
}

@Composable
private fun FaceBadge(face: FaceAssessment, modifier: Modifier = Modifier, size: Dp = 20.dp, iconSize: Dp = 14.dp, onClick: () -> Unit = {}) {
    val colors = FrameTheme.colors
    val (icon, tint, label) = when (face.level) {
        FaceLevel.Good -> Triple(if (face.smiling) FrameIcons.FaceSmile else FrameIcons.FaceGood, colors.success, QualityStrings.faceGood())
        FaceLevel.Poor -> Triple(FrameIcons.FacePoor, colors.error, QualityStrings.facePoor())
        else -> Triple(FrameIcons.FaceFair, colors.onSurfaceVariant, QualityStrings.faceFair())
    }
    Box(modifier.size(size).background(Color.Black.copy(alpha = 0.45f), FrameShapes.pill).frameClickable(onClick = onClick), Alignment.Center) {
        Image(
            painterResource(icon),
            label,
            Modifier.size(iconSize),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint)
        )
    }
}
