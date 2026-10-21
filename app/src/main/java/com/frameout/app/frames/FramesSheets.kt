package com.frameout.app.frames

import com.frameout.designsystem.glass.frameSheetSurface
import com.frameout.designsystem.glass.frameGlow
import androidx.compose.ui.graphics.Brush
import com.frameout.designsystem.theme.frameCardEdge
import com.frameout.designsystem.theme.frameCardBrush
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.frameout.app.settings.NavRow
import com.frameout.app.settings.SettingsCard
import com.frameout.app.settings.SettingsDivider
import com.frameout.app.settings.SettingsSection
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.QualityStrings
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.motion.frameEnter
import com.frameout.designsystem.motion.frameExit
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameHairline

private val SheetTopShape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
internal fun FramesSheet(visible: Boolean, title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = FrameTheme.colors
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.82f).dp
    BackHandler(enabled = visible, onBack = onClose)
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (FrameTheme.colors.isDark) 0.5f else 0.34f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
        )
    }
    AnimatedVisibility(visible, enter = frameEnter(), exit = frameExit(), modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), Alignment.BottomCenter) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .frameSheetSurface(SheetTopShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .navigationBarsPadding()
                    .padding(top = 8.dp)
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 4.dp).background(colors.primary.copy(alpha = 0.45f), FrameShapes.pill))
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FrameText(title, style = FrameTheme.typography.title, modifier = Modifier.weight(1f).screenHeading())
                    HeaderButton(FrameIcons.Close, QualityStrings.contentClose(), onClose)
                }
                Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 20.dp), content = content)
            }
        }
    }
}

@Composable
fun FramesToolsSheet(
    state: FramesState,
    onClose: () -> Unit,
    onSharpest: () -> Unit,
    onDuplicates: () -> Unit,
    onNoPeople: () -> Unit,
    onTags: () -> Unit,
    onRescan: () -> Unit,
    onIntegrity: () -> Unit,
    onSlides: () -> Unit,
    onVideo: () -> Unit,
    onGif: () -> Unit,
    onPdf: () -> Unit,
    onZip: () -> Unit,
    onCsv: () -> Unit,
    onEvidence: () -> Unit,
    extracting: Boolean = false,
    onContactSheet: (() -> Unit)? = null,
    onImportant: (() -> Unit)? = null
) {
    fun then(action: () -> Unit): () -> Unit = { onClose(); action() }
    val alert = state.integrity?.let { !it.healthy } == true
    FramesSheet(state.toolsOpen, QualityStrings.toolsTitle(), onClose) {
        SettingsSection(QualityStrings.toolsSelect()) {
            SettingsCard {
                onImportant?.let {
                    NavRow(FrameIcons.StarFilled, QualityStrings.important(), hint = QualityStrings.toolsImportantHint(), onClick = then(it))
                    SettingsDivider()
                }
                NavRow(FrameIconsExtra.Blur, QualityStrings.sharpest(), hint = QualityStrings.toolsSharpestHint(), onClick = then(onSharpest))
                SettingsDivider()
                NavRow(FrameIconsExtra.Duplicate, QualityStrings.duplicates(), hint = QualityStrings.toolsDuplicatesHint(), onClick = then(onDuplicates))
                SettingsDivider()
                NavRow(FrameIconsExtra.Person, QualityStrings.noPeople(), hint = QualityStrings.toolsNoPeopleHint(), onClick = then(onNoPeople))
            }
        }
        SettingsSection(QualityStrings.toolsAnalysis()) {
            SettingsCard {
                val tagValue = if (state.scanning) QualityStrings.contentScanProgress(state.scanDone, state.scanTotal, state.scanFailed) else state.tagCounts.size.takeIf { it > 0 }?.toString()
                NavRow(FrameIconsExtra.Album, QualityStrings.content(), hint = QualityStrings.toolsTagsHint(), value = tagValue, onClick = then(onTags))
                SettingsDivider()
                NavRow(FrameIconsExtra.Timer, QualityStrings.contentRescan(), hint = QualityStrings.toolsRescanHint(), onClick = then(onRescan))
                if (state.integrity != null) {
                    SettingsDivider()
                    NavRow(if (alert) FrameIconsExtra.Warning else FrameIcons.Info, QualityStrings.integrity(), hint = QualityStrings.toolsIntegrityHint(), value = if (alert) QualityStrings.integrityAlert() else null, onClick = then(onIntegrity))
                }
            }
        }
        if (!extracting) SettingsSection(QualityStrings.toolsExport()) {
            SettingsCard {
                NavRow(FrameIconsExtra.Live, FrameStrings.exportVideo(), onClick = then(onVideo))
                SettingsDivider()
                NavRow(FrameIconsExtra.Gif, FrameStrings.exportGif(), onClick = then(onGif))
                SettingsDivider()
                NavRow(FrameIconsExtra.ImageFormat, FrameStrings.exportPdf(), onClick = then(onPdf))
                SettingsDivider()
                onContactSheet?.let {
                    NavRow(FrameIconsExtra.ContactSheet, FrameStrings.exportContact(), hint = FrameStrings.contactHint(), onClick = then(it))
                    SettingsDivider()
                }
                NavRow(FrameIconsExtra.Storage, FrameStrings.exportZip(), onClick = then(onZip))
                SettingsDivider()
                NavRow(FrameIconsExtra.Storage, FrameStrings.exportCsv(), onClick = then(onCsv))
                SettingsDivider()
                NavRow(FrameIconsExtra.Warning, FrameStrings.exportEvidence(), onClick = then(onEvidence))
                SettingsDivider()
                NavRow(FrameIconsExtra.ImageFormat, QualityStrings.slidesPdf(), hint = QualityStrings.toolsSlidesHint(), onClick = then(onSlides))
            }
            FrameText(QualityStrings.toolsExportScope(), style = FrameTheme.typography.bodySmall, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
        }
    }
}
