package com.frameout.app.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.BuildConfig
import com.frameout.app.FrameOutApp
import com.frameout.app.billing.rememberProGate
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.library.IconButtonBox
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.DiagLog
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ModeStrings
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.dock.DockItem
import com.frameout.designsystem.dock.FrameDock
import com.frameout.designsystem.dock.FrameFab
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameAmbientGlow

@Composable
fun SettingsScreen(onBack: () -> Unit, onAbout: () -> Unit = {}, onLibrary: () -> Unit = {}, onAddVideo: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = (context.applicationContext as FrameOutApp).container.prefs
    val theme by prefs.themeMode.collectAsState()
    val lang by prefs.language.collectAsState()
    val format by prefs.saveFormat.collectAsState()
    val autoSave by prefs.autoSave.collectAsState()
    val datePolicy by prefs.datePolicy.collectAsState()
    val extract by prefs.extractMode.collectAsState()
    val folder by prefs.saveFolderUri.collectAsState()
    PreferredFrameTheme { SettingsContent(context, prefs, onBack, onAbout, onLibrary, onAddVideo, SettingsValues(theme, lang, format, autoSave, datePolicy, extract, folder)) }
}

internal data class SettingsValues(
    val theme: String, val lang: String, val format: String, val autoSave: Boolean, val datePolicy: String, val extract: String, val folder: String
)

@Composable
private fun SettingsContent(context: Context, prefs: AppPrefs, onBack: () -> Unit, onAbout: () -> Unit, onLibrary: () -> Unit, onAddVideo: () -> Unit, v: SettingsValues) {
    val colors = FrameTheme.colors
    var sheet by remember { mutableStateOf<SettingsSheet?>(null) }
    val pickFolder = rememberFolderPicker(prefs)
    val folderText by produceState(FrameStrings.defaultFolder(), v.folder) { value = withContext(Dispatchers.IO) { folderValue(context, v.folder) } }
    Box(Modifier.fillMaxSize().background(colors.background).frameAmbientGlow()) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SettingsHeader(onBack)
            ProSettings()
            AppearanceSettings(prefs, v.theme, v.lang) { sheet = SettingsSheet.Language }
            ExportSettings(prefs, folderText, v, { sheet = SettingsSheet.Format }) { sheet = SettingsSheet.Folder }
            ExtractionSettings(prefs, v.extract, { sheet = SettingsSheet.Mode }) { sheet = SettingsSheet.Content }
            SettingsSection(FrameStrings.diagnostics()) { SettingsCard { DiagnosticsSettings(context) } }
            Column(Modifier.padding(top = 12.dp)) {
                SettingsCard {
                    NavRow(FrameIcons.Info, ModeStrings.about(), value = BuildConfig.VERSION_NAME, monoValue = true, onClick = onAbout)
                }
            }
            Spacer(Modifier.height(120.dp))
        }
        SettingsDock(onLibrary, onAddVideo)
        SettingsSheets(sheet, prefs, context, v, pickFolder, onClose = { sheet = null })
    }
}

@Composable
private fun SettingsDock(onLibrary: () -> Unit, onAddVideo: () -> Unit) {
    FrameDock(
        items = listOf(
            DockItem("library", FrameStrings.library(), FrameIcons.Library, selectedIcon = FrameIcons.LibraryFilled, onClick = onLibrary),
            DockItem("settings", FrameStrings.settings(), FrameIcons.Settings, selectedIcon = FrameIcons.SettingsFilled, selected = true, onClick = {})
        ),
        fab = { FrameFab(FrameIcons.VideoAdd, FrameStrings.addVideo(), onAddVideo) },
        fabLabel = FrameStrings.addVideo(),
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButtonBox(FrameIcons.Back, FrameStrings.back(), onBack)
        FrameText(
            FrameStrings.settings(),
            style = FrameTheme.typography.display.copy(fontSize = 22.sp, lineHeight = 28.sp),
            modifier = Modifier.screenHeading()
        )
    }
}

@Composable
private fun AppearanceSettings(prefs: AppPrefs, theme: String, lang: String, onLanguage: () -> Unit) {
    val accent by prefs.accent.collectAsState()
    val iconFollows by prefs.iconFollowsTheme.collectAsState()
    SettingsSection(FrameStrings.appearance()) {
        SettingsCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingsRowHeader(FrameIconsExtra.Theme, FrameStrings.theme())
                ThemePicker(theme) { prefs.setThemeMode(it) }
            }
            SettingsDivider(16.dp)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingsRowHeader(FrameIconsExtra.Palette, FrameStrings.accentColor())
                AccentPicker(accent) { prefs.setAccent(it) }
            }
            SettingsDivider(16.dp)
            AppIconSettings(prefs, iconFollows, accent, theme)
            SettingsDivider(16.dp)
            NavRow(FrameIconsExtra.Language, FrameStrings.language(), value = languageLabel(lang), onClick = onLanguage)
        }
    }
}

@Composable
private fun ExportSettings(prefs: AppPrefs, folderText: String, v: SettingsValues, onFormat: () -> Unit, onFolder: () -> Unit) {
    val keepLocation by prefs.keepLocation.collectAsState()
    SettingsSection(FrameStrings.export()) {
        SettingsCard {
            NavRow(FrameIconsExtra.ImageFormat, FrameStrings.format(), value = FrameStrings.settingValue(v.format), onClick = onFormat)
            SettingsDivider()
            QualitySettings(prefs, v.format)
            SettingsDivider()
            NavRow(FrameIconsExtra.Folder, FrameStrings.saveFolder(), value = folderText, onClick = onFolder)
            SettingsDivider()
            ToggleRow(FrameIcons.Save, FrameStrings.autoSave(), FrameStrings.autoSaveHint(), v.autoSave) { prefs.setAutoSave(it) }
            SettingsDivider()
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingsRowHeader(FrameIconsExtra.Calendar, FrameStrings.datePolicy())
                SettingsSegmented(settingOptions(listOf("frame", "video")), v.datePolicy) { prefs.setDatePolicy(it) }
            }
            SettingsDivider()
            ToggleRow(FrameIconsExtra.Location, FrameStrings.keepLocation(), FrameStrings.keepLocationHint(), keepLocation) { prefs.setKeepLocation(it) }
        }
    }
}

@Composable
private fun ExtractionSettings(prefs: AppPrefs, extract: String, onMode: () -> Unit, onContent: () -> Unit) {
    val skipBlurry by prefs.skipBlurry.collectAsState()
    val skipDuplicates by prefs.skipDuplicates.collectAsState()
    val locked = rememberProGate().locked(false)
    SettingsSection(FrameStrings.extraction(), FrameStrings.settingsApplyNotice()) {
        SettingsCard { ExtractModeSettings(prefs, extract, onMode, onContent) }
        Spacer(Modifier.height(12.dp))
        SettingsCard {
            ToggleRow(FrameIconsExtra.Blur, FrameStrings.skipBlurryTitle(), ModeStrings.helpBlurry(), skipBlurry, locked) { gateSmart(it, "blurry") { prefs.setSkipBlurry(it) } }
            SettingsDivider()
            ToggleRow(FrameIconsExtra.Duplicate, FrameStrings.skipDuplicatesTitle(), ModeStrings.helpDuplicates(), skipDuplicates, locked) { gateSmart(it, "dup") { prefs.setSkipDuplicates(it) } }
            SettingsDivider()
            VolumeNavSettings(prefs)
        }
    }
}

@Composable
private fun DiagnosticsSettings(context: Context) {
    NavRow(FrameIcons.Share, FrameStrings.exportLog(), hint = FrameStrings.diagHint()) {
        val file = DiagLog.export(context)
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(Intent.createChooser(send, FrameStrings.exportLog()))
    }
}
