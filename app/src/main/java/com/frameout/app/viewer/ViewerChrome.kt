package com.frameout.app.viewer

import androidx.compose.ui.graphics.Color
import com.frameout.designsystem.theme.frameControl
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import com.frameout.designsystem.theme.FrameShapes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.Dp
import com.frameout.designsystem.theme.rememberFrameAdaptive
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameHoldScrub
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme

@Composable
internal fun ViewerChrome(
    state: ViewerState,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onSearch: () -> Unit,
    onPlay: () -> Unit,
    onSpeed: () -> Unit,
    onEdit: (EditTool) -> Unit,
    onToggleDetections: () -> Unit,
    onToggleLiveText: () -> Unit,
    onLiveCopy: () -> Unit,
    onLiveSelectAll: () -> Unit,
    onPrevious: (Int) -> Unit,
    onNext: (Int) -> Unit,
    arrows: ArrowWake,
    tools: ViewerToolEditor,
    onShowInProject: () -> Unit = {}
) {
    val frame = state.current
    val sideRail = rememberFrameAdaptive().sideEditor
    var scrub by remember { mutableIntStateOf(1) }
    val boost = when { state.total > 2000 -> 3; state.total > 600 -> 2; else -> 1 }
    val liveActive = frame != null && !state.editing && state.liveText.active && !state.playing && state.bitmap != null
    Box(Modifier.fillMaxSize()) {
        if (frame != null) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ChromePill {
                        FrameText(if (state.total > 0) "${state.position} / ${state.total}" else "#${frame.index + 1}", style = FrameTheme.typography.mono.copy(shadow = androidx.compose.ui.graphics.Shadow(ViewerInk.accent.copy(alpha = 0.65f), androidx.compose.ui.geometry.Offset.Zero, 10f)), color = ViewerInk.accent)
                        Box(Modifier.size(width = 1.dp, height = 14.dp).background(ViewerInk.text.copy(alpha = 0.28f)))
                        FrameText(formatPts(frame.ptsUs), style = FrameTheme.typography.mono, color = ViewerInk.text)
                    }
                }
                val showDetections = state.detections.isNotEmpty()
                val showText = !state.playing && state.bitmap != null
                if (!state.editing && !liveActive) {
                    ChromeStrip {
                        if (!state.savedMode) StripSegment(onClick = onSpeed, description = FrameStrings.playbackFps(state.playFps)) {
                            FrameText("${state.playFps} fps", style = FrameTheme.typography.label.copy(fontSize = 12.sp), color = ViewerInk.text)
                        }
                        if (showDetections) {
                            if (!state.savedMode) StripDivider()
                            val label = QualityStrings.detectionsLabel(state.detections.size).latinDigits()
                            StripSegment(onClick = onToggleDetections, selected = state.showDetections, description = label) {
                                Image(
                                    painter = painterResource(FrameIconsExtra.Detect),
                                    contentDescription = label,
                                    modifier = Modifier.size(18.dp),
                                    colorFilter = ColorFilter.tint(if (state.showDetections) ViewerInk.accent else ViewerInk.text)
                                )
                                FrameText(
                                    "${state.detections.size}",
                                    style = FrameTheme.typography.label.copy(fontSize = 12.sp),
                                    color = if (state.showDetections) ViewerInk.accent else ViewerInk.text,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                        if (showText) {
                            if (!state.savedMode || showDetections) StripDivider()
                            StripSegment(onClick = onToggleLiveText, description = QualityStrings.liveTextLabel(), square = true) {
                                Image(
                                    painter = painterResource(FrameIconsExtra.Text),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    colorFilter = ColorFilter.tint(ViewerInk.text)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (frame != null && !state.editing) {
            if (liveActive) LiveTextBar(state.liveText, onToggleLiveText, onLiveCopy, onLiveSelectAll)
            ViewerScrubBadge(scrub, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 60.dp))
            if (!state.savedMode && !state.playing && state.tool == EditTool.None) {
                Box(Modifier.fillMaxSize().padding(state.imagePadding())) {
                    Row(
                        Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 6.dp).graphicsLayer { alpha = arrows.alpha.value },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SideArrow(FrameStrings.previous(), state.canGoPrevious, arrows.interactive, true, boost, { scrub = it }) { arrows.poke(); onPrevious(it) }
                        SideArrow(FrameStrings.next(), state.canGoNext, arrows.interactive, false, boost, { scrub = it }) { arrows.poke(); onNext(it) }
                    }
                }
            }
        }
        if (state.tool != EditTool.None) ViewerToolPanel(state, tools)
        else if (sideRail) ViewerDock(
            onSave = onSave,
            onDelete = onDelete,
            onShare = onShare,
            onSearch = onSearch,
            onEdit = { onEdit(EditTool.Adjust) },
            playing = state.playing,
            onPlay = onPlay,
            showPlay = !state.savedMode,
            showGoTo = state.savedMode,
            onGoTo = onShowInProject
        ) else ViewerBottomBar(
            playing = state.playing,
            onPlay = onPlay,
            onSave = onSave,
            onShare = onShare,
            onSearch = onSearch,
            onDelete = onDelete,
            onEdit = { onEdit(EditTool.Adjust) },
            modifier = Modifier.align(Alignment.BottomCenter),
            showPlay = !state.savedMode,
            showGoTo = state.savedMode,
            onGoTo = onShowInProject
        )
    }
}

@Composable
internal fun CropOverlayBox(state: ViewerState, vm: ViewerViewModel) {
    val bitmap = state.bitmap ?: return
    val rect = state.displayCrop()
    Box(Modifier.fillMaxSize().padding(state.panelPadding())) {
        CropOverlay(
            cropL = rect.l,
            cropT = rect.t,
            cropR = rect.r,
            cropB = rect.b,
            aspect = bitmap.width.toFloat() / bitmap.height,
            onChange = { l, t, r, b -> vm.tools.updateCrop(l, t, r, b) }
        )
    }
}

@Composable
internal fun ChromePill(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    ViewerGlassSurface(modifier = modifier) {
        Row(
            Modifier.defaultMinSize(minHeight = 44.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
internal fun ChromeStrip(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    ViewerGlassSurface(modifier = modifier) {
        Row(
            Modifier.height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
internal fun StripSegment(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    square: Boolean = false,
    description: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    val accent = ViewerInk.accent
    val tint = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent
    Row(
        modifier
            .fillMaxHeight()
            .padding(2.dp)
            .then(if (selected) Modifier.viewerActiveGlow(accent) else Modifier)
            .clip(FrameShapes.pill)
            .background(tint)
            .frameClickable(onClickLabel = description, onClick = onClick)
            .then(if (square) Modifier.width(44.dp) else Modifier.padding(horizontal = 10.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
internal fun StripDivider() {
    Box(Modifier.size(width = 1.dp, height = 14.dp).background(ViewerInk.text.copy(alpha = 0.28f)))
}

@Composable
private fun SideArrow(label: String, enabled: Boolean, awake: Boolean, flip: Boolean, boost: Int, onSpeed: (Int) -> Unit, onStep: (Int) -> Unit) {
    ViewerGlassSurface(shape = CircleShape, modifier = Modifier.size(44.dp).alpha(if (enabled) 1f else 0.3f)) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).then(if (enabled && awake) Modifier.frameHoldScrub(boost, onSpeed, onStep) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(FrameIconsExtra.Chevron),
                contentDescription = label,
                modifier = Modifier.size(22.dp).graphicsLayer { if (flip) rotationZ = 180f },
                colorFilter = ColorFilter.tint(ViewerInk.accent)
            )
        }
    }
}

internal val ViewerBarZone: Dp = 12.dp + 60.dp + 12.dp

internal fun String.latinDigits(): String = map { if (it in '\u0660'..'\u0669') '0' + (it - '\u0660') else it }.joinToString("")
