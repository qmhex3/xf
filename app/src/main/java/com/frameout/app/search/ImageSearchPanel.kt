package com.frameout.app.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.frameout.app.settings.BottomSheetFrame
import com.frameout.app.settings.NavRow
import com.frameout.app.settings.SettingsCard
import com.frameout.app.settings.SettingsDivider
import com.frameout.core.search.ImageSearchEngines
import com.frameout.core.search.SearchEngine
import com.frameout.core.util.SearchStrings
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme

private fun engineIcon(engine: SearchEngine): Int = when (engine.id) {
    ImageSearchEngines.LENS_ID -> FrameIconsExtra.Fit
    "yandex_images" -> FrameIcons.Search
    "bing_visual" -> FrameIconsExtra.Zoom
    ImageSearchEngines.CHOOSER_ID -> FrameIcons.Share
    else -> FrameIconsExtra.Duplicate
}

@Composable
fun ImageSearchPanel(visible: Boolean, onPick: (SearchEngine) -> Unit, onClose: () -> Unit) {
    val lang = com.frameout.core.util.AppLang.of(LocalConfiguration.current.locales[0].toLanguageTag())
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.7f).dp
    BottomSheetFrame(visible, onClose, SearchStrings.title()) {
        Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            FrameText(
                SearchStrings.sheetHint(),
                style = FrameTheme.typography.bodySmall,
                color = FrameTheme.colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            )
            SettingsCard(Modifier.padding(top = 10.dp, bottom = 8.dp)) {
                ImageSearchEngines.all.forEachIndexed { index, engine ->
                    if (index > 0) SettingsDivider()
                    NavRow(
                        icon = engineIcon(engine),
                        title = engine.name,
                        hint = engine.desc(lang),
                        value = if (engine.id == ImageSearchEngines.LENS_ID) SearchStrings.recommended() else null,
                        onClick = { onPick(engine) }
                    )
                }
            }
        }
    }
}
