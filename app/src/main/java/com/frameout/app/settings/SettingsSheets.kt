package com.frameout.app.settings

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frameout.app.billing.ProBadge
import com.frameout.app.billing.ProCheck
import com.frameout.app.library.SearchField
import com.frameout.core.pro.ProCatalog
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.storage.ImageFormat
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.LocaleHelper
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

internal enum class SettingsSheet { Language, Format, Mode, Folder }

internal class ChoiceOption(val key: String, val label: String, val caption: String? = null, val tag: String? = null, val pro: Boolean = false)

internal class SheetSpec(
    val title: String,
    val options: List<ChoiceOption>,
    val selected: String,
    val searchHint: String? = null,
    val onSelect: (String) -> Unit
)

private const val SEARCH_THRESHOLD = 7

@Composable
internal fun SettingsSheets(sheet: SettingsSheet?, prefs: AppPrefs, context: Context, v: SettingsValues, onPickFolder: () -> Unit, onClose: () -> Unit) {
    val holder = remember { arrayOfNulls<SheetSpec>(1) }
    val choice = sheet != null && sheet != SettingsSheet.Mode && sheet != SettingsSheet.Folder
    sheet?.takeIf { choice }?.let { holder[0] = specFor(it, prefs, context, v, onClose) }
    holder[0]?.let { ChoiceSheet(choice, it, onClose) }
    ModeSheet(sheet == SettingsSheet.Mode, prefs, v.extract, onClose)
    SaveFolderSheet(sheet == SettingsSheet.Folder, prefs, v.folder, onPickFolder, onClose)
}

private fun specFor(sheet: SettingsSheet, prefs: AppPrefs, context: Context, v: SettingsValues, onClose: () -> Unit): SheetSpec = when (sheet) {
    SettingsSheet.Language -> SheetSpec(FrameStrings.pickLanguage(), languageOptions(), v.lang, FrameStrings.searchLanguage()) { key ->
        onClose()
        if (key != v.lang) {
            prefs.setLanguage(key)
            LocaleHelper.apply(context, key)
            (context as? Activity)?.recreate()
        }
    }
    SettingsSheet.Format -> SheetSpec(
        FrameStrings.format(),
        ImageFormat.availableKeys().map { ChoiceOption(it, FrameStrings.settingValue(it), pro = ProCheck.locked(ProCatalog.formatIsFree(ImageFormat.fromKey(it)))) },
        v.format
    ) { key ->
        onClose()
        val free = com.frameout.core.pro.ProCatalog.formatIsFree(ImageFormat.fromKey(key))
        com.frameout.core.pro.ProAccess.source?.gate(com.frameout.core.pro.ProFeature.ExportFormats, free, key) { prefs.setSaveFormat(key) } ?: prefs.setSaveFormat(key)
    }
    SettingsSheet.Mode, SettingsSheet.Folder -> error("Shown by its own sheet")
}

@Composable
internal fun ChoiceSheet(visible: Boolean, spec: SheetSpec, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(visible) { if (!visible) query = "" }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
    val searchable = spec.searchHint != null && spec.options.size > SEARCH_THRESHOLD
    val shown = if (query.isBlank()) spec.options else spec.options.filter {
        it.label.contains(query.trim(), ignoreCase = true) || it.key.contains(query.trim(), ignoreCase = true)
    }
    BottomSheetFrame(visible, onClose, spec.title) {
        if (searchable) {
            SearchField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), spec.searchHint.orEmpty())
        }
        ChoiceList(shown, spec, maxHeight)
    }
}

@Composable
private fun ChoiceList(shown: List<ChoiceOption>, spec: SheetSpec, maxHeight: androidx.compose.ui.unit.Dp) {
    Column(
        Modifier
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        shown.forEach { option -> ChoiceItem(option, option.key == spec.selected) { spec.onSelect(option.key) } }
        if (shown.isEmpty()) {
            FrameText(
                FrameStrings.noResults(),
                color = FrameTheme.colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        }
    }
}

@Composable
internal fun ChoiceItem(option: ChoiceOption, selected: Boolean, onClick: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FrameShapes.md)
            .then(if (selected) Modifier.background(colors.primary.copy(alpha = 0.12f)) else Modifier)
            .frameSelectable(selected, Role.RadioButton, onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FrameText(
                option.label,
                style = FrameTheme.typography.body.copy(fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal),
                color = if (selected) colors.primary else colors.onSurface
            )
            option.caption?.let { FrameText(it, style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
        option.tag?.let { FrameText(it, style = FrameTheme.typography.monoSmall, color = colors.onSurfaceVariant) }
        if (option.pro) ProBadge()
        RadioMark(selected)
    }
}

@Composable
internal fun RadioMark(selected: Boolean) {
    val ring = FrameTheme.colors.onSurfaceVariant.copy(alpha = 0.45f)
    Box(
        Modifier
            .size(22.dp)
            .clip(FrameShapes.pill)
            .then(if (selected) Modifier.background(FrameGradients.Brand) else Modifier.border(1.5.dp, ring, FrameShapes.pill)),
        Alignment.Center
    ) {
        if (selected) Image(painterResource(FrameIcons.Check), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(Color.White))
    }
}
