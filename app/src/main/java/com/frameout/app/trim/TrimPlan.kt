package com.frameout.app.trim

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.app.extract.formatExtractTime
import com.frameout.core.util.ModeStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameCardBrush
import com.frameout.designsystem.theme.frameCardEdge
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractModeCodec

internal fun planText(mode: ExtractMode): String = when (mode) {
    ExtractMode.EveryFrame -> TrimStrings.planAll()
    is ExtractMode.EveryNFrames -> TrimStrings.planEveryN(mode.n)
    is ExtractMode.EveryNMillis -> TrimStrings.planEveryTime(timeText(mode.millis))
    is ExtractMode.FixedCount -> TrimStrings.planFixed(mode.count)
    ExtractMode.KeyframesOnly -> TrimStrings.planKeyframes()
    ExtractMode.SceneChange -> TrimStrings.planScene()
    ExtractMode.Slides -> TrimStrings.planSlides()
}

private fun timeText(millis: Long): String =
    if (millis % 1000L == 0L) formatExtractTime(millis) else "$millis ${ModeStrings.millis()}"

@Composable
internal fun PlanButton(env: TrimEnv, modifier: Modifier) {
    val stored by env.prefs.extractMode.collectAsState()
    val colors = FrameTheme.colors
    val mode = ExtractModeCodec.decode(stored)
    val frames = estimateFrames(env.player.durationMs, env.clamped, stored)
    val megabytes = estimateMb(env.player.durationMs, env.clamped, stored, env.player)
    val shape = FrameShapes.lg
    Box(
        modifier
            .height(56.dp)
            .frameGlow(colors.accentA, { if (colors.isDark) 0.45f else 0.2f }, 8.dp, { 0.43f })
            .clip(shape)
            .background(frameCardBrush(colors.accentA, 0.12f))
            .border(1.dp, frameCardEdge(colors.accentA, 0.5f), shape)
            .frameClickable(onClickLabel = TrimStrings.planChange()) { env.onSheet(TrimSheet.Plan) }
    ) {
        PlanButtonRow(mode, frames, megabytes)
        PlanMeter(
            megabytes,
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 5.dp).height(3.dp)
        )
    }
}

@Composable
private fun PlanButtonRow(mode: ExtractMode, frames: Int, megabytes: Float) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
        Arrangement.spacedBy(8.dp),
        Alignment.CenterVertically
    ) {
        PlanModeTile(mode, 36.dp)
        Column(Modifier.weight(1f), Arrangement.spacedBy(0.dp, Alignment.CenterVertically)) {
            FrameText(
                planText(mode),
                style = FrameTheme.typography.titleSmall.copy(textDirection = TextDirection.Content),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MiniStat(FrameIconsExtra.Frames, planCount(frames), colors.onSurfaceVariant)
                MiniStat(FrameIconsExtra.Storage, planMegabytes(megabytes) + " " + planSizeUnit(), colors.primary)
            }
        }
        PlanAdjustChip()
    }
}

@Composable
internal fun PlanWarning(env: TrimEnv) {
    if (!env.autoSave) return
    val stored by env.prefs.extractMode.collectAsState()
    val frames = estimateFrames(env.player.durationMs, env.clamped, stored)
    FrameText(TrimStrings.galleryWarn(frames), style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.error, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
internal fun PlanSummary(env: TrimEnv, compact: Boolean, tappable: Boolean) {
    val stored by env.prefs.extractMode.collectAsState()
    val colors = FrameTheme.colors
    val mode = ExtractModeCodec.decode(stored)
    val frames = estimateFrames(env.player.durationMs, env.clamped, stored)
    val megabytes = estimateMb(env.player.durationMs, env.clamped, stored, env.player)
    val click = if (tappable) Modifier.frameClickable { env.onSheet(TrimSheet.Plan) } else Modifier
    val shape = FrameShapes.xl
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(frameCardBrush(colors.accentA, 0.14f))
            .border(1.dp, frameCardEdge(colors.accentA, 0.5f), shape)
            .then(click)
            .padding(14.dp),
        Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
            Column(Modifier.weight(1f), Arrangement.spacedBy(2.dp)) {
                if (!compact) FrameText(TrimStrings.planTitle(), style = FrameTheme.typography.label, color = colors.onSurfaceVariant)
                FrameText(planText(mode), style = FrameTheme.typography.titleSmall, maxLines = 2)
            }
            if (tappable) PlanChangeButton()
        }
        PlanFilmStrip(mode, height = if (compact) 48.dp else 56.dp)
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            PlanStat(FrameIconsExtra.Frames, planCount(frames), planFramesUnit(frames), Modifier.weight(1f))
            PlanStat(FrameIconsExtra.Storage, planMegabytes(megabytes), planSizeUnit(), Modifier.weight(1f))
        }
        PlanMeter(megabytes, Modifier.fillMaxWidth().height(4.dp))
    }
}

@Composable
internal fun PlanModeTile(mode: ExtractMode, size: Dp) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.sm
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.accentA.copy(alpha = 0.34f), colors.accentB.copy(alpha = 0.14f))))
            .border(1.dp, colors.accentA.copy(alpha = 0.35f), shape),
        Alignment.Center
    ) {
        PlanStrip(mode, Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 6.dp), cells = 6, perforations = false)
    }
}

@Composable
private fun MiniStat(icon: Int, text: String, tint: Color) {
    Row(Modifier, Arrangement.spacedBy(3.dp), Alignment.CenterVertically) {
        Image(painterResource(icon), null, Modifier.size(12.dp), colorFilter = ColorFilter.tint(tint))
        FrameText(
            text,
            style = FrameTheme.typography.label.copy(textDirection = TextDirection.Content),
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun PlanAdjustChip() {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(colors.accentA.copy(alpha = 0.16f))
            .border(1.dp, colors.accentA.copy(alpha = 0.30f), CircleShape),
        Alignment.Center
    ) {
        Image(painterResource(FrameIconsExtra.Adjust), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(colors.primary))
    }
}

@Composable
internal fun PlanChangeButton() {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.accentA.copy(alpha = 0.16f))
            .border(1.dp, colors.accentA.copy(alpha = 0.30f), CircleShape)
            .semantics { contentDescription = TrimStrings.planChange() },
        Alignment.Center
    ) {
        Image(painterResource(FrameIconsExtra.Adjust), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(colors.primary))
    }
}
