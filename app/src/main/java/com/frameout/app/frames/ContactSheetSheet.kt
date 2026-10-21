package com.frameout.app.frames

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.app.settings.SettingsActionButton
import com.frameout.app.settings.SettingsCard
import com.frameout.app.settings.SettingsDivider
import com.frameout.app.settings.SettingsIconTile
import com.frameout.app.settings.ToggleRow
import com.frameout.app.settings.rowTitleStyle
import com.frameout.core.export.ContactSheetLayout
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SavedStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

private fun AppPrefs.contactOptions() = ContactSheetLayout.Options(contactColumns(), contactShowTime(), contactShowNumber(), contactPng())

@Composable
internal fun ContactSheetSheet(visible: Boolean, frameCount: Int, prefs: AppPrefs, onClose: () -> Unit, forSaved: Boolean = false, onCreate: (ContactSheetLayout.Options) -> Unit) {
    var options by remember { mutableStateOf(prefs.contactOptions()) }
    LaunchedEffect(visible) { if (visible) options = prefs.contactOptions() }
    FramesSheet(visible, if (forSaved) SavedStrings.exportContact() else FrameStrings.contactTitle(), onClose) {
        ContactHint(forSaved, frameCount)
        SettingsCard {
            ColumnsRow(options.safeColumns) {
                prefs.setContactColumns(it)
                options = options.copy(columns = it)
            }
            SettingsDivider()
            if (!forSaved) {
                ToggleRow(FrameIconsExtra.Timer, FrameStrings.contactShowTime(), FrameStrings.contactShowTimeHint(), options.showTime) {
                    prefs.setContactShowTime(it)
                    options = options.copy(showTime = it)
                }
                SettingsDivider()
            }
            ToggleRow(FrameIconsExtra.Duplicate, FrameStrings.contactShowNumber(), FrameStrings.contactShowNumberHint(), options.showNumber) {
                prefs.setContactShowNumber(it)
                options = options.copy(showNumber = it)
            }
            SettingsDivider()
            ToggleRow(FrameIconsExtra.ImageFormat, FrameStrings.contactPng(), FrameStrings.contactPngHint(), options.png) {
                prefs.setContactPng(it)
                options = options.copy(png = it)
            }
        }
        Box(Modifier.padding(top = 14.dp)) {
            SettingsActionButton(FrameStrings.contactCreate()) {
                onClose()
                onCreate(options)
            }
        }
    }
}

@Composable
private fun ContactHint(forSaved: Boolean, count: Int) {
    FrameText(
        contactHintText(forSaved, count),
        style = FrameTheme.typography.bodySmall,
        color = FrameTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

private fun contactHintText(forSaved: Boolean, count: Int): String = when {
    forSaved -> SavedStrings.exportContactSheetHint() + if (count > 0) "  ·  " + FrameStrings.selectedCount(count) else ""
    else -> FrameStrings.contactHint() + if (count > 0) "  ·  " + FrameStrings.frameCount(count) else ""
}

@Composable
private fun ColumnsRow(columns: Int, onChange: (Int) -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsIconTile(FrameIcons.GridSize)
        FrameText(FrameStrings.contactColumns(), style = rowTitleStyle(), modifier = Modifier.weight(1f))
        StepButton(FrameIconsExtra.Minus, ContactSheetLayout.MIN_COLUMNS < columns, FrameStrings.gridColumns((columns - 1).coerceAtLeast(ContactSheetLayout.MIN_COLUMNS))) { onChange(columns - 1) }
        FrameText(columns.toString(), style = FrameTheme.typography.mono, color = colors.primary, textAlign = TextAlign.Center, modifier = Modifier.size(width = 28.dp, height = 24.dp))
        StepButton(FrameIconsExtra.Plus, columns < ContactSheetLayout.MAX_COLUMNS, FrameStrings.gridColumns((columns + 1).coerceAtMost(ContactSheetLayout.MAX_COLUMNS))) { onChange(columns + 1) }
    }
}

@Composable
private fun StepButton(icon: Int, enabled: Boolean, description: String, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Box(
        Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(FrameShapes.pill)
            .background(colors.container)
            .then(if (enabled) Modifier.frameClickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = description },
        Alignment.Center
    ) {
        Image(painterResource(icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.onSurface))
    }
}
