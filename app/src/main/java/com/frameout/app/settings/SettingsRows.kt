package com.frameout.app.settings

import androidx.compose.animation.animateColorAsState
import com.frameout.app.billing.ProBadge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameToggle
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.designsystem.theme.frameHairline

@Composable
internal fun rowTitleStyle(): TextStyle = FrameTheme.typography.body.copy(fontWeight = FontWeight.Medium)

@Composable
internal fun SettingsSection(title: String, caption: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = FrameTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        FrameText(
            title,
            style = FrameTheme.typography.label,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp).screenHeading()
        )
        caption?.let {
            FrameText(it, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp))
        }
        Column(Modifier.padding(top = 10.dp), content = content)
    }
}

@Composable
internal fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(FrameShapes.xl)
            .background(FrameTheme.colors.fieldFill)
            .border(1.dp, frameHairline(), FrameShapes.xl),
        content = content
    )
}

@Composable
internal fun SettingsDivider(inset: Dp = 64.dp) {
    Box(Modifier.fillMaxWidth().padding(start = inset).height(1.dp).background(frameHairline()))
}

@Composable
internal fun SettingsIconTile(icon: Int, active: Boolean = false) {
    val colors = FrameTheme.colors
    val background by animateColorAsState(
        if (active) colors.primary.copy(alpha = 0.16f) else colors.onSurface.copy(alpha = 0.06f),
        frameSpring(),
        label = "tileBackground"
    )
    val tint by animateColorAsState(if (active) colors.primary else colors.onSurfaceVariant, frameSpring(), label = "tileTint")
    Box(Modifier.size(36.dp).clip(FrameShapes.sm).background(background), Alignment.Center) {
        Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(tint))
    }
}

@Composable
internal fun SettingsRowHeader(icon: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsIconTile(icon)
        FrameText(title, style = rowTitleStyle())
    }
}

@Composable
internal fun ToggleRow(icon: Int, title: String, hint: String?, checked: Boolean, proMark: Boolean = false, onChange: (Boolean) -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().frameToggle(checked, onChange).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsIconTile(icon, checked)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FrameText(title, style = rowTitleStyle(), modifier = Modifier.weight(1f, fill = false))
                if (proMark) ProBadge()
            }
            hint?.let { FrameText(it, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
        SettingsSwitch(checked)
    }
}

@Composable
internal fun NavRow(
    icon: Int,
    title: String,
    hint: String? = null,
    value: String? = null,
    monoValue: Boolean = false,
    onClick: () -> Unit
) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().frameClickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsIconTile(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FrameText(title, style = rowTitleStyle())
            hint?.let { FrameText(it, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
        value?.let {
            FrameText(
                it,
                style = if (monoValue) FrameTheme.typography.monoSmall else FrameTheme.typography.label,
                color = if (monoValue) colors.onSurfaceVariant else colors.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 150.dp)
            )
        }
        Image(painterResource(FrameIconsExtra.Chevron), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.onSurfaceVariant))
    }
}

@Composable
internal fun SettingsHint(text: String) {
    FrameText(
        text,
        style = FrameTheme.typography.bodySmall,
        color = FrameTheme.colors.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 64.dp, end = 16.dp, bottom = 12.dp)
    )
}

@Composable
internal fun SettingsActionButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(FrameShapes.md)
            .background(FrameGradients.Brand)
            .frameClickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        Alignment.Center
    ) {
        FrameText(label, style = FrameTheme.typography.label, color = Color.White, textAlign = TextAlign.Center)
    }
}
