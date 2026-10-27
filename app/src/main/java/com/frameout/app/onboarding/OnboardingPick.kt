package com.frameout.app.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.quality.TagNames
import com.frameout.core.quality.TagRules
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SmoothScale
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.frameGlow
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline
import java.util.Locale
import kotlin.math.max

internal const val PickHeightFactor = 1.02f
internal val PickExtraHeight = 0.dp

@Composable
internal fun PickStage(width: Dp, modifier: Modifier = Modifier) {
    val path = rememberFirstClip(OnboardingClips.PICK, OnboardingClips.FORMATS, OnboardingClips.HERO)
    val phase = rememberLoop(durationMs = 8000, still = 0.95f, reverse = false, label = "pick")
    val frames = if (path != null) rememberClipFrames(path, BurstAt, 240, 180) else null
    val blurred = remember(frames) {
        frames?.let { list -> BlurCells.associateWith { i -> list.getOrNull(i)?.let(::blurredCopy) } }
    }
    val colors = FrameTheme.colors
    val shape = RoundedCornerShape(24.dp)
    val rim = Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.40f), frameHairline(), colors.container))
    Column(
        modifier
            .width(width)
            .clearAndSetSemantics { }
            .frameGlow(colors.primary, { 0.18f }, 22.dp, { 0.07f })
            .clip(shape)
            .background(Brush.verticalGradient(listOf(OnbColors.Ink, OnbColors.Night)))
            .border(1.dp, rim, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PickHeader()
        Divider()
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) { col -> BurstCell(row * 3 + col, frames, blurred, phase, Modifier.weight(1f)) }
                    }
                }
            }
        }
        Divider()
        PickFooter(phase)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
}

private fun blurredCopy(src: ImageBitmap): ImageBitmap {
    val bitmap = src.asAndroidBitmap()
    val small = SmoothScale.scaled(bitmap, max(1, bitmap.width / 12), max(1, bitmap.height / 12), true)
    return SmoothScale.scaled(small, bitmap.width, bitmap.height, true).asImageBitmap()
}

@Composable
private fun PickHeader() {
    val pulse by rememberLoop(durationMs = 1000, still = 1f, reverse = true, label = "pulse", ease = true)
    val mono = FrameTheme.typography.monoSmall.copy(fontSize = 10.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = 0.4f + 0.6f * pulse }
                    .clip(FrameShapes.pill)
                    .background(OnbColors.CyanSoft)
            )
            FrameText(
                FrameStrings.onboardingPickAnalysis(),
                style = FrameTheme.typography.titleSmall.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            FrameText(
                FrameStrings.onboardingPickBurst(Cells),
                style = mono,
                color = OnDark,
                maxLines = 1,
                modifier = Modifier
                    .clip(FrameShapes.xs)
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(12.dp)) { drawSparkle(OnbColors.CyanSoft) }
            FrameText(FrameStrings.onboardingPickLocal(), style = mono.copy(fontSize = 11.sp), color = OnbColors.CyanSoft, maxLines = 1)
        }
    }
}

@Composable
private fun PickFooter(phase: State<Float>) {
    val clarity by remember {
        derivedStateOf { if (phase.value >= 0.97f) 0 else (ease(seg(phase.value, 0.74f, 0.90f)) * 994f).toInt() }
    }
    val excluded by remember {
        derivedStateOf {
            val p = phase.value
            if (p >= 0.97f) 0 else (if (p >= 0.38f) 2 else 0) + (if (p >= 0.42f) 1 else 0) + (if (p >= 0.46f) 1 else 0)
        }
    }
    val mono = FrameTheme.typography.monoSmall.copy(fontSize = 11.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(FrameIcons.Check), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(OnbColors.CyanSoft))
            FrameText(FrameStrings.onboardingPickClarity(), style = mono, color = OnDark, maxLines = 1)
            FrameText(
                String.format(Locale.US, "%d.%d%%", clarity / 10, clarity % 10),
                style = mono.copy(fontWeight = FontWeight.Bold),
                color = OnbColors.CyanSoft,
                maxLines = 1
            )
        }
        FrameText(
            FrameStrings.onboardingPickExcluded(excluded),
            style = FrameTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = OnDark,
            maxLines = 1
        )
    }
}

@Composable
internal fun PickChips(modifier: Modifier = Modifier) {
    val lang = com.frameout.core.util.AppLang.of(LocalConfiguration.current.locales[0].toLanguageTag())
    val labels = remember(lang) {
        listOf(TagRules.GOOD_FACE, TagRules.EYES_CLOSED, TagRules.TEXT, TagRules.CODE, "l:Sunset", "l:Sea")
            .map { TagNames.display(it, lang) }
    }
    CyclingChips(labels, null, modifier)
}
