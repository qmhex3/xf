package com.frameout.app.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.frameout.core.util.FrameStrings
import com.frameout.app.billing.ProBadge
import com.frameout.app.billing.rememberProGate
import com.frameout.core.pro.ProCatalog
import com.frameout.core.pro.ProFeature
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.motion.rememberThemeWaveSource
import com.frameout.designsystem.theme.FrameAccent
import com.frameout.designsystem.theme.FrameAccents
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.resolveFrameAccent

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
        gate.gated(ProFeature.Accents, key == FrameAccents.DYNAMIC || ProCatalog.accentIsFree(key), key) { applied(); onChange(key) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        options.forEach { accent -> AccentSwatch(accent, accent.key == value, guarded, gate.locked(accent.key == FrameAccents.DYNAMIC || ProCatalog.accentIsFree(accent.key))) }
    }
}

@Composable
private fun AccentSwatch(accent: FrameAccent, selected: Boolean, onChange: (String, () -> Unit) -> Unit, locked: Boolean) {
    val wave = rememberThemeWaveSource()
    val name = FrameStrings.settingValue(accent.key)
    val fill = remember(accent) { Brush.linearGradient(listOf(accent.a, accent.b, accent.c)) }
    val ring = if (selected) FrameTheme.colors.onSurface else Color.Transparent
    val checkTint = if (accent.b.luminance() > 0.5f) Color.Black else Color.White
    Box(
        Modifier
            .size(44.dp)
            .then(wave.modifier)
            .clip(CircleShape)
            .semantics { contentDescription = name }
            .frameSelectable(selected, Role.RadioButton) {
                onChange(accent.key) { if (!selected) wave.fire(accent.b) }
            }
            .border(2.dp, ring, CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(fill),
        Alignment.Center
    ) {
        if (locked) ProBadge()
        if (selected) {
            Image(painterResource(FrameIcons.Check), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(checkTint))
        }
    }
}
