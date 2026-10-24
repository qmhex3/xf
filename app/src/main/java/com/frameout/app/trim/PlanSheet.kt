package com.frameout.app.trim

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import com.frameout.designsystem.theme.FrameGradients
import com.frameout.designsystem.theme.frameHairline
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.frameout.app.settings.ExtractModePicker
import com.frameout.app.settings.gateMode
import com.frameout.app.settings.gateSmart
import com.frameout.app.settings.SettingsNumberField
import com.frameout.app.settings.SegmentOption
import com.frameout.app.settings.SettingsSegmented
import com.frameout.app.settings.SettingsSwitch
import com.frameout.app.settings.ToggleRow
import com.frameout.app.settings.kindOf
import com.frameout.app.settings.modeFor
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.ContentStrings
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ModeStrings
import com.frameout.core.util.TrimStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.frameSelectable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.fieldFill
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractModeCodec
import com.frameout.engine.extract.decidesContent

private const val MAX_FRAMES = 100000L
private const val MAX_SECONDS = 86400L
private const val MODE_FACES = "faces"
private const val MODE_BOTH = "both"

@Composable
internal fun BoxScope.PlanSheet(env: TrimEnv, visible: Boolean) {
    val stored by env.prefs.extractMode.collectAsState()
    var contentOpen by rememberSaveable { mutableStateOf(false) }
    val content = ExtractModeCodec.decode(stored) as? ExtractMode.Content
    if (contentOpen && content != null) {
        ContentOptionsSheet(visible, env.prefs, content) { contentOpen = false }
        return
    }
    TrimSheetHost(visible, TrimStrings.optionsTitle(), { env.onSheet(TrimSheet.None) }) {
        ExtractOptions(
            env.prefs, env.autoSave, env.onAutoSave, env.frameEstimate(stored),
            summary = { PlanSummary(env, compact = true, tappable = false) },
            onDone = { env.onSheet(TrimSheet.None) },
            onContent = { contentOpen = true }
        )
    }
}

@Composable
internal fun BoxScope.ContentOptionsSheet(visible: Boolean, prefs: AppPrefs, mode: ExtractMode.Content, onBack: () -> Unit) {
    TrimSheetHost(visible, ContentStrings.title(), onBack) {
        com.frameout.app.settings.ContentSheetBody(mode.spec, { prefs.setExtractMode(ExtractModeCodec.encode(ExtractMode.Content(it))) })
        DoneButton(onBack)
    }
}

@Composable
internal fun ColumnScope.ExtractOptions(
    prefs: AppPrefs, autoSave: Boolean, onAutoSave: () -> Unit, frames: Int,
    summary: @Composable () -> Unit, onDone: () -> Unit, onContent: () -> Unit = {}
) {
    val stored by prefs.extractMode.collectAsState()
    val mode = ExtractModeCodec.decode(stored)
    summary()
    SectionLabel(TrimStrings.optionsMode())
    ExtractModePicker(kindOf(mode), { kind -> gateMode(kind) { prefs.setExtractMode(ExtractModeCodec.encode(modeFor(kind, mode))) } })
    ModeParameter(mode, prefs, onContent)
    SmartFilters(prefs, mode)
    GalleryToggle(autoSave, onAutoSave, frames)
    DoneButton(onDone)
}

@Composable
private fun SectionLabel(text: String) {
    FrameText(text, style = FrameTheme.typography.label, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(FrameShapes.md).background(FrameTheme.colors.fieldFill).border(1.dp, frameHairline(), FrameShapes.md),
        content = content
    )
}

@Composable
private fun DoneButton(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(52.dp).clip(FrameShapes.pill).background(FrameGradients.Brand).frameClickable(onClick = onClick),
        Alignment.Center
    ) {
        FrameText(FrameStrings.done(), style = FrameTheme.typography.titleSmall, color = Color.White)
    }
}

@Composable
private fun ModeParameter(mode: ExtractMode, prefs: AppPrefs, onContent: () -> Unit) {
    val save = { next: ExtractMode -> prefs.setExtractMode(ExtractModeCodec.encode(next)) }
    if (mode is ExtractMode.Content) {
        SectionCard {
            com.frameout.app.settings.NavRow(FrameIconsExtra.Detect, ContentStrings.title(), value = com.frameout.app.settings.contentSummary(mode.spec, FrameStrings.lang()), onClick = onContent)
        }
        return
    }
    if (mode !is ExtractMode.EveryNFrames && mode !is ExtractMode.FixedCount && mode !is ExtractMode.EveryNMillis) return
    key(kindOf(mode)) { SectionCard {
        when (mode) {
            is ExtractMode.EveryNFrames -> SettingsNumberField(ModeStrings.framesLabel(), mode.n.toLong(), MAX_FRAMES) { save(ExtractMode.EveryNFrames(it.toInt())) }
            is ExtractMode.FixedCount -> SettingsNumberField(ModeStrings.countLabel(), mode.count.toLong(), MAX_FRAMES) { save(ExtractMode.FixedCount(it.toInt())) }
            is ExtractMode.EveryNMillis -> SettingsNumberField(ModeStrings.timeLabel(), (mode.millis / 1000L).coerceAtLeast(1L), MAX_SECONDS) { save(ExtractMode.EveryNMillis(it * 1000L)) }
            else -> Unit
        }
    } }
}

@Composable
private fun SmartFilters(prefs: AppPrefs, mode: ExtractMode) {
    val colors = FrameTheme.colors
    val blurry by prefs.skipBlurry.collectAsState()
    val duplicates by prefs.skipDuplicates.collectAsState()
    val peopleChosen by prefs.peopleOnly.collectAsState()
    val peopleIgnored = mode.decidesContent()
    val people = peopleChosen && !peopleIgnored
    val body by prefs.peopleBody.collectAsState()
    SectionLabel(TrimStrings.filtersTitle())
    SectionCard {
        ToggleRow(FrameIconsExtra.Blur, FrameStrings.skipBlurryTitle(), ModeStrings.helpBlurry(), blurry) { gateSmart(it, "blurry") { prefs.setSkipBlurry(it) } }
        ToggleRow(FrameIconsExtra.Duplicate, FrameStrings.skipDuplicatesTitle(), ModeStrings.helpDuplicates(), duplicates) { gateSmart(it, "dup") { prefs.setSkipDuplicates(it) } }
        ToggleRow(FrameIconsExtra.Person, TrimStrings.peopleOnlyTitle(), TrimStrings.peopleOnlyHelp(), peopleChosen) { gateSmart(it, "people") { prefs.setPeopleOnly(it) } }
    }
    if (peopleChosen && peopleIgnored) FrameText(TrimStrings.peopleIgnored(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
    if (people) {
        val options = listOf(SegmentOption(MODE_FACES, TrimStrings.peopleModeFaces()), SegmentOption(MODE_BOTH, TrimStrings.peopleModeBoth()))
        SettingsSegmented(options, if (body) MODE_BOTH else MODE_FACES) { prefs.setPeopleBody(it == MODE_BOTH) }
        FrameText(TrimStrings.peopleSlow(), style = FrameTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(horizontal = 4.dp))
    }
    if (blurry || duplicates || people) FrameText(TrimStrings.filtersCountHint(), style = FrameTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun GalleryToggle(autoSave: Boolean, onAutoSave: () -> Unit, frames: Int) {
    val colors = FrameTheme.colors
    SectionCard {
        ToggleRow(FrameIconsExtra.Album, TrimStrings.gallerySave(), TrimStrings.galleryHint(), autoSave) { onAutoSave() }
    }
    if (autoSave) FrameText(TrimStrings.galleryWarn(frames), style = FrameTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(horizontal = 4.dp))
}
