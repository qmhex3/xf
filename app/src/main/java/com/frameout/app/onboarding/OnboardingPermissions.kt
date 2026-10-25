package com.frameout.app.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.oem.OemSettings
import com.frameout.app.oem.OemStepsCard
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.motion.frameFadeIn
import com.frameout.designsystem.motion.frameFadeOut
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private enum class Glyph { Media, Bell, Bolt }

@Composable
internal fun PermissionPanel(perms: ReadyPermissions, modifier: Modifier = Modifier) {
    val colors = FrameTheme.colors
    val shape = FrameShapes.lg
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.container.copy(alpha = 0.9f))
            .border(1.dp, frameHairline(), shape)
    ) {
        PermissionRow(Glyph.Media, FrameStrings.onboardingPermMedia(), FrameStrings.onboardingPermMediaDesc(), perms.media, perms.requestMedia)
        RowDivider()
        PermissionRow(Glyph.Bell, FrameStrings.onboardingPermNotif(), FrameStrings.onboardingPermNotifDesc(), perms.notifications, perms.requestNotifications)
        RowDivider()
        PermissionRow(Glyph.Bolt, FrameStrings.onboardingPermBattery(), FrameStrings.onboardingPermBatteryDesc(), perms.battery, perms.requestBattery)
        if (OemSettings.isCustom(perms.oem)) {
            RowDivider()
            OemStepsCard(perms.oem, Modifier.padding(14.dp), onOpenBattery = perms.openBatteryList)
        }
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(frameHairline()))
}

@Composable
private fun PermissionRow(glyph: Glyph, title: String, description: String, enabled: Boolean, onAllow: () -> Unit) {
    val colors = FrameTheme.colors
    val tint = if (enabled) colors.success else colors.onSurfaceVariant
    val tile = FrameShapes.md
    Row(
        Modifier.fillMaxWidth().padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(tile)
                .background(if (enabled) colors.success.copy(alpha = 0.10f) else colors.containerHigh.copy(alpha = 0.6f))
                .border(1.dp, if (enabled) colors.success.copy(alpha = 0.25f) else frameHairline(), tile),
            contentAlignment = Alignment.Center
        ) {
            GlyphIcon(glyph, tint, Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FrameText(title, style = FrameTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
            FrameText(
                description,
                style = FrameTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = colors.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
        StatusPill(enabled, onAllow)
    }
}

@Composable
private fun StatusPill(enabled: Boolean, onAllow: () -> Unit) {
    val colors = FrameTheme.colors
    val fadeIn = frameFadeIn()
    val fadeOut = frameFadeOut()
    AnimatedContent(targetState = enabled, transitionSpec = { fadeIn togetherWith fadeOut }, label = "status") { on ->
        if (on) {
            Row(
                Modifier
                    .clip(FrameShapes.pill)
                    .background(colors.success.copy(alpha = 0.10f))
                    .border(1.dp, colors.success.copy(alpha = 0.30f), FrameShapes.pill)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painterResource(FrameIcons.Check), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(colors.success))
                FrameText(
                    FrameStrings.onboardingPermEnabled(),
                    style = FrameTheme.typography.titleSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.success,
                    maxLines = 1
                )
            }
        } else {
            FrameText(
                FrameStrings.notifAllow(),
                style = FrameTheme.typography.titleSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                modifier = Modifier
                    .clip(FrameShapes.pill)
                    .frameClickable(onClick = onAllow)
                    .background(colors.containerHigh)
                    .border(1.dp, frameHairline(), FrameShapes.pill)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun GlyphIcon(glyph: Glyph, tint: Color, modifier: Modifier) {
    when (glyph) {
        Glyph.Media -> Image(painterResource(FrameIcons.Library), null, modifier, colorFilter = ColorFilter.tint(tint))
        Glyph.Bell -> Canvas(modifier) { drawBell(tint) }
        Glyph.Bolt -> Canvas(modifier) { drawBolt(tint) }
    }
}

private fun DrawScope.drawBell(color: Color) {
    val w = size.width
    val h = size.height
    val body = Path().apply {
        moveTo(w * 0.18f, h * 0.74f)
        cubicTo(w * 0.30f, h * 0.64f, w * 0.30f, h * 0.52f, w * 0.32f, h * 0.42f)
        cubicTo(w * 0.34f, h * 0.26f, w * 0.66f, h * 0.26f, w * 0.68f, h * 0.42f)
        cubicTo(w * 0.70f, h * 0.52f, w * 0.70f, h * 0.64f, w * 0.82f, h * 0.74f)
        close()
    }
    drawPath(body, color)
    drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.5f, h * 0.85f))
    drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.20f))
}

private fun DrawScope.drawBolt(color: Color) {
    val w = size.width
    val h = size.height
    val bolt = Path().apply {
        moveTo(w * 0.58f, h * 0.06f)
        lineTo(w * 0.24f, h * 0.56f)
        lineTo(w * 0.47f, h * 0.56f)
        lineTo(w * 0.40f, h * 0.94f)
        lineTo(w * 0.76f, h * 0.42f)
        lineTo(w * 0.53f, h * 0.42f)
        close()
    }
    drawPath(bolt, color)
}
