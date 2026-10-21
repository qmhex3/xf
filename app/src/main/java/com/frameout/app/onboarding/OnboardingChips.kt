package com.frameout.app.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SettingStrings
import com.frameout.designsystem.a11y.LocalSystemA11y
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

@Composable
internal fun ModeChips(modifier: Modifier = Modifier) {
    CyclingChips(OnboardingModeKinds.map { SettingStrings.value(it) }, FrameStrings.onboardingExtract(), modifier)
}

@Composable
internal fun CyclingChips(labels: List<String>, description: String?, modifier: Modifier = Modifier) {
    val reduceMotion = LocalSystemA11y.current.reduceMotion
    val listState = rememberLazyListState()
    val loop = rememberLoop(durationMs = labels.size * 1700, still = 0f, reverse = false, label = "chips")
    val active by remember { derivedStateOf { (loop.value * labels.size).toInt().coerceIn(0, labels.size - 1) } }
    LaunchedEffect(active) { if (!reduceMotion) listState.animateScrollToItem(active) }
    val semanticsModifier = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth().then(semanticsModifier),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(labels) { i, label -> ModeChip(label, i == active) }
    }
}

@Composable
private fun ModeChip(label: String, active: Boolean) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.pill
    val fill: Brush = if (active) {
        Brush.horizontalGradient(listOf(OnbColors.Indigo.copy(alpha = 0.28f), OnbColors.Violet.copy(alpha = 0.28f)))
    } else {
        SolidColor(colors.container)
    }
    Row(
        Modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, if (active) OnbColors.Violet.copy(alpha = 0.7f) else frameHairline(), shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (active) Box(Modifier.size(6.dp).clip(FrameShapes.pill).background(OnbColors.CyanSoft))
        FrameText(
            label,
            style = FrameTheme.typography.titleSmall.copy(fontSize = 13.sp),
            color = if (active) colors.onSurface else colors.onSurfaceVariant
        )
    }
}
