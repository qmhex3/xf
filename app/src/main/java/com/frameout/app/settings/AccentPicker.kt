package com.frameout.app.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frameout.app.BuildConfig
import com.frameout.app.billing.ProGate
import com.frameout.app.billing.proBrush
import com.frameout.app.billing.proOnBrush
import com.frameout.app.billing.rememberProGate
import com.frameout.core.ads.AdStrings
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProStrings
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.motion.rememberThemeWaveSource
import com.frameout.designsystem.theme.FrameAccent
import com.frameout.designsystem.theme.FrameAccents
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.resolveFrameAccent

private const val SWATCH_DP = 52
private const val COLUMNS = 4

private enum class Lock { Open, Ad, Pro }

private fun lockOf(key: String, gate: ProGate): Lock = when {
    !gate.enforced || gate.isPro || ProCatalog.accentIsFree(key) -> Lock.Open
    ProCatalog.accentIsProOnly(key) || !BuildConfig.ADS_ENABLED -> Lock.Pro
    gate.rewarded(ProFeature.Accents) -> Lock.Open
    else -> Lock.Ad
}

@Composable
internal fun AccentPicker(value: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val options = remember {
        buildList {
            addAll(FrameAccents.All)
            if (FrameAccents.dynamicSupported()) add(resolveFrameAccent(FrameAccents.DYNAMIC, context))
        }
    }
    val gate = rememberProGate()
    val guarded: (String, () -> Unit) -> Unit = { key, applied ->
        val apply = { applied(); onChange(key) }
        when (lockOf(key, gate)) {
            Lock.Open -> apply()
            Lock.Pro -> gate.showPro()
            Lock.Ad -> gate.run(ProFeature.Accents, key, apply)
        }
    }
    val locks = options.map { lockOf(it.key, gate) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        options.chunked(COLUMNS).forEachIndexed { rowIndex, rowItems ->
            Row(Modifier.fillMaxWidth()) {
                rowItems.forEachIndexed { col, accent ->
                    Box(Modifier.weight(1f), Alignment.TopCenter) {
                        AccentSwatch(accent, accent.key == value, guarded, locks[rowIndex * COLUMNS + col])
                    }
                }
                repeat(COLUMNS - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (locks.any { it != Lock.Open }) Legend(locks.any { it == Lock.Ad })
    }
}

@Composable
private fun Legend(showAd: Boolean) {
    val muted = FrameTheme.colors.onSurfaceVariant
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showAd) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(22.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(alpha = 0.58f)), Alignment.Center) { CapIcon(Lock.Ad, 8) }
                FrameText(AdStrings.rewardWatch(), style = FrameTheme.typography.bodySmall, color = muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(22.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(proBrush()), Alignment.Center) { CapIcon(Lock.Pro, 8) }
            FrameText(ProStrings.badge(), style = FrameTheme.typography.bodySmall, color = muted)
        }
    }
}

@Composable
private fun CapIcon(lock: Lock, size: Int) {
    val pro = lock == Lock.Pro
    Image(
        painterResource(if (pro) FrameIcons.StarFilled else FrameIcons.PlayFilled),
        null,
        Modifier.size(size.dp),
        colorFilter = ColorFilter.tint(if (pro) proOnBrush() else Color.White)
    )
}

@Composable
private fun BoxScope.GlassCap(lock: Lock) {
    val fill = if (lock == Lock.Pro) proBrush() else Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.58f), Color.Black.copy(alpha = 0.58f)))
    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(15.dp).background(fill), Alignment.Center) { CapIcon(lock, 9) }
}

@Composable
private fun AccentSwatch(accent: FrameAccent, selected: Boolean, onChange: (String, () -> Unit) -> Unit, lock: Lock) {
    val wave = rememberThemeWaveSource()
    val name = FrameStrings.settingValue(accent.key)
    val state = when (lock) {
        Lock.Open -> ""
        Lock.Ad -> ", " + AdStrings.rewardWatch()
        Lock.Pro -> ", " + ProStrings.badge()
    }
    val fill = remember(accent) { Brush.linearGradient(listOf(accent.a, accent.b, accent.c)) }
    val ring = if (selected) FrameTheme.colors.onSurface else Color.Transparent
    val checkTint = if (accent.b.luminance() > 0.5f) Color.Black else Color.White
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(SWATCH_DP.dp)
                .then(wave.modifier)
                .clip(CircleShape)
                .semantics { contentDescription = name + state }
                .frameSelectable(selected, Role.RadioButton) {
                    onChange(accent.key) { if (!selected) wave.fire(accent.b) }
                }
                .border(2.dp, ring, CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(fill),
            Alignment.Center
        ) {
            if (lock != Lock.Open) GlassCap(lock)
            if (selected) {
                Image(painterResource(FrameIcons.Check), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(checkTint))
            }
        }
        FrameText(name, style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant, maxLines = 1)
    }
}
