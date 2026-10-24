package com.frameout.app.update

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.core.util.UpdateStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.contentColorFor

private val ButtonShape = RoundedCornerShape(18.dp)

@Composable
internal fun UpdateBody(info: UpdateInfo, onLater: () -> Unit, onUpdate: () -> Unit, onShowFixes: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    Column(modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 22.dp)) {
        FrameText(
            UpdateStrings.title(),
            style = FrameTheme.typography.title.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.screenHeading()
        )
        FrameText(
            UpdateStrings.meta(info.versionName, info.sizeLabel),
            style = FrameTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp)
        )
        Column(Modifier.padding(top = 8.dp)) {
            info.features.forEach { FeatureRow(it) }
        }
        info.fixes?.let { FixesRow(it, onShowFixes) }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LaterButton(onLater, Modifier.weight(1f))
            UpdateButton(onUpdate, Modifier.weight(1.6f))
        }
    }
}

@Composable
private fun FeatureRow(feature: UpdateFeature) {
    val colors = FrameTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(44.dp).background(colors.primaryContainer, RoundedCornerShape(14.dp)), Alignment.Center) {
            Image(painterResource(feature.icon), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(colors.primary))
        }
        Column(Modifier.weight(1f)) {
            FrameText(feature.title, style = FrameTheme.typography.label.copy(fontWeight = FontWeight.Bold))
            FrameText(feature.body, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun FixesRow(fixes: UpdateFixes, onClick: (() -> Unit)?) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.md
    val click = if (onClick != null) Modifier.frameClickable(onClick = onClick) else Modifier
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(shape)
            .background(colors.success.copy(alpha = if (colors.isDark) 0.16f else 0.11f))
            .then(click)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(30.dp).background(colors.success, FrameShapes.pill), Alignment.Center) {
            Image(painterResource(FrameIcons.Check), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(contentColorFor(colors.success)))
        }
        Column(Modifier.weight(1f)) {
            FrameText(UpdateStrings.fixesTitle(fixes.count), style = FrameTheme.typography.label.copy(fontWeight = FontWeight.Bold), color = colors.success)
            FrameText(fixes.body, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        if (onClick != null) {
            Image(painterResource(FrameIconsExtra.Chevron), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(colors.success))
        }
    }
}

@Composable
private fun LaterButton(onClick: () -> Unit, modifier: Modifier) {
    val colors = FrameTheme.colors
    Box(
        modifier
            .heightIn(min = 54.dp)
            .clip(ButtonShape)
            .background(colors.onSurface.copy(alpha = 0.07f))
            .frameClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        Alignment.Center
    ) {
        FrameText(UpdateStrings.later(), style = FrameTheme.typography.label, textAlign = TextAlign.Center)
    }
}

@Composable
private fun UpdateButton(onClick: () -> Unit, modifier: Modifier) {
    val colors = FrameTheme.colors
    Row(
        modifier
            .heightIn(min = 54.dp)
            .clip(ButtonShape)
            .background(colors.primary)
            .frameClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Image(painterResource(FrameIconsExtra.Download), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(colors.onPrimary))
        FrameText(UpdateStrings.now(), style = FrameTheme.typography.label.copy(fontWeight = FontWeight.Bold), color = colors.onPrimary)
    }
}
