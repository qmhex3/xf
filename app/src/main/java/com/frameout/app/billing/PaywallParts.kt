package com.frameout.app.billing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

@Composable
fun PaywallHead(modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val title = FrameTheme.typography.display.copy(
        brush = Brush.horizontalGradient(listOf(colors.onSurface, colors.onSurface, colors.primary, colors.accentC)),
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Column(modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Kicker()
        BasicText(ProStrings.title(), Modifier.padding(top = 12.dp, bottom = 6.dp).semantics { heading() }, title)
        FrameText(ProStrings.subtitle(), FrameTheme.typography.body, colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun Kicker() {
    val colors = FrameTheme.colors
    Row(
        Modifier
            .background(colors.primary.copy(alpha = 0.13f), FrameShapes.pill)
            .border(1.dp, colors.primary.copy(alpha = 0.28f), FrameShapes.pill)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(FrameIcons.StarFilled), null, Modifier.size(11.dp), colorFilter = ColorFilter.tint(colors.primary))
        FrameText(ProStrings.badge(), FrameTheme.typography.label.copy(fontSize = 11.sp), colors.primary, Modifier.padding(start = 5.dp))
    }
}

internal fun featureIcon(feature: ProFeature): Int = when (feature) {
    ProFeature.SmartExtract -> FrameIconsExtra.Detect
    ProFeature.Filters -> FrameIcons.Filter
    ProFeature.Shapes -> FrameIconsExtra.Shapes
    ProFeature.EditTools -> FrameIconsExtra.Brush
    ProFeature.ExportFormats -> FrameIconsExtra.ImageFormat
    ProFeature.AdvancedExport -> FrameIconsExtra.ExportOut
    ProFeature.Collage -> FrameIcons.Collage
    ProFeature.Batch -> FrameIcons.VideoAdd
    ProFeature.Compare -> FrameIconsExtra.Adjust
    ProFeature.Accents -> FrameIconsExtra.Palette
    ProFeature.AppIcons -> FrameIconsExtra.Cover
    ProFeature.Insights -> FrameIcons.FaceGood
    ProFeature.General -> FrameIcons.StarFilled
}

@Composable
fun PaywallContext(feature: ProFeature, trialsLeft: Int?, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val general = feature == ProFeature.General
    val title = if (general) ProStrings.title() else ProStrings.trying(ProStrings.featureName(feature))
    val detail = ProStrings.featureLine(feature) + trialsSuffix(trialsLeft)
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.primary.copy(alpha = 0.11f), FrameShapes.md)
            .border(1.dp, colors.primary.copy(alpha = 0.30f), FrameShapes.md)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp).clip(FrameShapes.sm).background(proBrush()), Alignment.Center) {
            Image(painterResource(featureIcon(feature)), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(proOnBrush()))
        }
        Column(Modifier.weight(1f)) {
            FrameText(title, FrameTheme.typography.titleSmall)
            FrameText(detail, FrameTheme.typography.bodySmall, colors.onSurfaceVariant, Modifier.padding(top = 2.dp))
        }
    }
}

private fun trialsSuffix(trialsLeft: Int?): String = when {
    trialsLeft == null -> ""
    trialsLeft > 0 -> "\n" + ProStrings.trialsLeft(trialsLeft)
    else -> "\n" + ProStrings.noTrials()
}

@Composable
fun FeatureChip(text: String) {
    val colors = FrameTheme.colors
    Row(
        Modifier
            .background(colors.onSurface.copy(alpha = 0.06f), FrameShapes.pill)
            .border(1.dp, frameHairline(), FrameShapes.pill)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).background(Brush.linearGradient(listOf(colors.accentC, colors.accentB)), FrameShapes.pill))
        FrameText(text, FrameTheme.typography.bodySmall, colors.onSurface, Modifier.padding(start = 7.dp), maxLines = 1, softWrap = false)
    }
}

@Composable
fun PaywallHeader(feature: ProFeature, trialsLeft: Int?, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val general = feature == ProFeature.General
    val title = FrameTheme.typography.display.copy(
        brush = Brush.horizontalGradient(listOf(colors.onSurface, colors.onSurface, colors.primary, colors.accentC)),
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Column(modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Kicker()
        BasicText(if (general) ProStrings.title() else ProStrings.featureName(feature), Modifier.padding(top = 10.dp, bottom = 4.dp).semantics { heading() }, title)
        val detail = if (general) ProStrings.subtitle() else ProStrings.featureLine(feature)
        FrameText(detail + trialsSuffix(trialsLeft), FrameTheme.typography.bodySmall, colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
