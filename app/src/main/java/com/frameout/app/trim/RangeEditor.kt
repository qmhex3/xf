package com.frameout.app.trim

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameHoldAccelerate
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline
import com.frameout.engine.timeline.TrimRange

private val NudgeSize = 32.dp
private val NudgeHit = 38.dp
private val RowHeight = 40.dp
private val PinHeight = 32.dp
private val PresetHeight = 34.dp
private val EdgeHeight = 12.dp + PinHeight + RowHeight
private val LENGTH_FIELD_WIDTH = 92.dp

@Composable
internal fun RangeEditor(env: TrimEnv) {
    val colors = FrameTheme.colors
    val duration = env.player.durationMs
    val base = env.clamped
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            Modifier
                .fillMaxWidth()
                .alpha(if (duration > 0L) 1f else 0.5f)
                .clip(FrameShapes.md)
                .background(colors.fieldFill)
                .border(1.dp, frameHairline(), FrameShapes.md)
        ) {
            Row(Modifier.fillMaxWidth()) {
                EdgeColumn(
                    FrameStrings.from(), TrimStrings.setStart(), FrameIconsExtra.MarkStart, base.startMs, Modifier.weight(1f),
                    onPin = { commitStart(env, base, env.controller.positionNow(), seek = false) },
                    onShift = { frames -> commitStart(env, base, base.startMs + frames * RangeEdit.NUDGE_MS, seek = true) },
                    onType = { ms -> commitStart(env, base, ms, seek = true) }
                )
                Box(Modifier.width(1.dp).height(EdgeHeight).background(frameHairline()))
                EdgeColumn(
                    FrameStrings.to(), TrimStrings.setEnd(), FrameIconsExtra.MarkEnd, base.endMs, Modifier.weight(1f),
                    onPin = { commitEnd(env, base, env.controller.positionNow(), seek = false) },
                    onShift = { frames -> commitEnd(env, base, base.endMs + frames * RangeEdit.NUDGE_MS, seek = true) },
                    onType = { ms -> commitEnd(env, base, ms, seek = true) }
                )
            }
        }
    }
}

private fun commitStart(env: TrimEnv, base: TrimRange, ms: Long, seek: Boolean) {
    val duration = env.player.durationMs
    if (duration <= 0L) return
    val next = TrimLimits.moveStart(base, ms, duration, env.output.maxMs)
    env.onRange(next)
    if (seek) env.controller.seekTo(next.startMs)
}

private fun commitEnd(env: TrimEnv, base: TrimRange, ms: Long, seek: Boolean) {
    val duration = env.player.durationMs
    if (duration <= 0L) return
    val next = TrimLimits.moveEnd(base, ms, duration, env.output.maxMs)
    env.onRange(next)
    if (seek) env.controller.seekTo(next.endMs)
}

private fun commitLength(env: TrimEnv, base: TrimRange, lengthMs: Long) {
    val duration = env.player.durationMs
    if (duration <= 0L) return
    val next = RangeEdit.withLength(base, duration, env.output.maxMs, lengthMs)
    env.onRange(next)
    val at = env.controller.positionNow()
    if (at < next.startMs || at > next.endMs) env.controller.seekTo(next.startMs)
}

private fun latinDigits(text: String): String = buildString(text.length) {
    text.forEach { c ->
        append(
            when (c) {
                in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
                in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
                else -> c
            }
        )
    }
}

@Composable
private fun EdgeColumn(
    label: String,
    pinLabel: String,
    pinIcon: Int,
    valueMs: Long,
    modifier: Modifier,
    onPin: () -> Unit,
    onShift: (Int) -> Unit,
    onType: (Long) -> Unit
) {
    val colors = FrameTheme.colors
    Column(modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            FrameText(label, style = FrameTheme.typography.label.copy(textDirection = TextDirection.Content), color = colors.onSurfaceVariant, maxLines = 1)
            PinButton(pinLabel, pinIcon, onPin)
        }
        Row(Modifier.fillMaxWidth().height(RowHeight), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            NudgeKey(FrameIconsExtra.Minus, "$label: ${TrimStrings.nudgeEarlier()}") { onShift(-it) }
            TimeEntry(valueMs, Modifier.weight(1f), flat = true, onCommit = onType)
            NudgeKey(FrameIconsExtra.Plus, "$label: ${TrimStrings.nudgeLater()}") { onShift(it) }
        }
    }
}

@Composable
private fun PinButton(label: String, icon: Int, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier
            .height(PinHeight)
            .clip(FrameShapes.pill)
            .background(colors.primary.copy(alpha = 0.20f))
            .frameClickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        Alignment.CenterVertically
    ) {
        Image(painterResource(icon), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(colors.primary))
        FrameText(label, style = FrameTheme.typography.label.copy(textDirection = TextDirection.Content, fontWeight = FontWeight.SemiBold), color = colors.primary, maxLines = 1)
    }
}

@Composable
private fun NudgeKey(icon: Int, description: String, onStep: (Int) -> Unit) {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .size(NudgeHit, RowHeight)
            .frameHoldAccelerate(onStep)
            .semantics { contentDescription = description },
        Alignment.Center
    ) {
        Box(Modifier.size(NudgeSize).clip(CircleShape).background(colors.surface), Alignment.Center) {
            Image(painterResource(icon), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(colors.onSurface))
        }
    }
}

@Composable
private fun LengthRow(env: TrimEnv, base: TrimRange) {
    val colors = FrameTheme.colors
    val presets = RangeEdit.presets(env.player.durationMs, env.output.maxMs)
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
        Row(Modifier, Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
            Image(painterResource(FrameIconsExtra.Timer), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(colors.onSurfaceVariant))
            FrameText(TrimStrings.rangeLength(), style = FrameTheme.typography.label.copy(textDirection = TextDirection.Content), color = colors.onSurfaceVariant, maxLines = 1)
            TimeEntry(base.spanMs(), Modifier.width(LENGTH_FIELD_WIDTH), flat = true, emphasis = true) { ms -> commitLength(env, base, ms) }
        }
        Box(Modifier.weight(1f), Alignment.CenterEnd) {
            Row(Modifier.horizontalScroll(rememberScrollState()), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
                presets.forEach { preset ->
                    val label = latinDigits(TrimStrings.presetSeconds((preset / 1000L).toInt()))
                    PresetChip(label, RangeEdit.matches(env.clamped.copy(useFull = false), preset), env.player.durationMs > 0L) { commitLength(env, base, preset) }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.sm
    Box(
        Modifier
            .height(PresetHeight)
            .widthIn(min = 46.dp)
            .clip(shape)
            .background(if (selected) colors.primary.copy(alpha = 0.20f) else colors.surface)
            .then(if (enabled) Modifier.frameClickable(onClickLabel = label, onClick = onClick) else Modifier)
            .semantics { contentDescription = label }
            .padding(horizontal = 10.dp),
        Alignment.Center
    ) {
        FrameText(
            label,
            style = FrameTheme.typography.label.copy(textDirection = TextDirection.Content, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) colors.primary else colors.onSurface,
            maxLines = 1
        )
    }
}
