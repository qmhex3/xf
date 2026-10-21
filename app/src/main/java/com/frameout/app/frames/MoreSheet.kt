package com.frameout.app.frames

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frameout.app.settings.NavRow
import com.frameout.app.settings.SettingsCard
import com.frameout.app.settings.SettingsDivider
import com.frameout.app.settings.SettingsIconTile
import com.frameout.app.settings.SettingsSection
import com.frameout.app.settings.rowTitleStyle
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.SearchStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme

internal enum class MoreGroup { Transform, Edit, Share, Export, Danger }

internal data class MoreAction(
    val group: MoreGroup,
    val label: String,
    val icon: Int,
    val onClick: () -> Unit
)

internal fun moreActions(
    deletedView: Boolean,
    canSearch: Boolean,
    onRestore: () -> Unit,
    onFavorite: () -> Unit,
    onRotate: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onCropAll: () -> Unit,
    onCompare: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSearch: () -> Unit,
    onReassemble: () -> Unit,
    onGif: () -> Unit,
    onPdf: () -> Unit,
    onZip: () -> Unit,
    onCsv: () -> Unit,
    onEvidence: () -> Unit,
    onDelete: () -> Unit,
    onEmptyTrash: (() -> Unit)? = null,
    extracting: Boolean = false,
    onContactSheet: (() -> Unit)? = null
): List<MoreAction> {
    if (deletedView) return buildList {
        add(MoreAction(MoreGroup.Edit, FrameStrings.restore(), FrameIconsExtra.Resume, onRestore))
        add(MoreAction(MoreGroup.Danger, FrameStrings.deletePermanently(), FrameIcons.Trash, onDelete))
        onEmptyTrash?.let { add(MoreAction(MoreGroup.Danger, FrameStrings.emptyTrash(), FrameIcons.Trash, it)) }
    }
    return buildList {
        add(MoreAction(MoreGroup.Transform, FrameStrings.rotate(), FrameIconsExtra.Rotate, onRotate))
        add(MoreAction(MoreGroup.Transform, FrameStrings.flipH(), FrameIconsExtra.FlipH, onFlipH))
        add(MoreAction(MoreGroup.Transform, FrameStrings.flipV(), FrameIconsExtra.FlipV, onFlipV))
        add(MoreAction(MoreGroup.Transform, FrameStrings.cropAll(), FrameIcons.Crop, onCropAll))
        add(MoreAction(MoreGroup.Edit, FrameStrings.undo(), FrameIconsExtra.Undo, onUndo))
        add(MoreAction(MoreGroup.Edit, FrameStrings.redo(), FrameIconsExtra.Redo, onRedo))
        add(MoreAction(MoreGroup.Edit, FrameStrings.favorite(), FrameIcons.Star, onFavorite))
        add(MoreAction(MoreGroup.Edit, FrameStrings.compare(), FrameIconsExtra.Cover, onCompare))
        if (canSearch) add(MoreAction(MoreGroup.Share, SearchStrings.action(), FrameIcons.Search, onSearch))
        if (!extracting) {
            add(MoreAction(MoreGroup.Export, FrameStrings.exportVideo(), FrameIconsExtra.Live, onReassemble))
            add(MoreAction(MoreGroup.Export, FrameStrings.exportGif(), FrameIconsExtra.Gif, onGif))
            add(MoreAction(MoreGroup.Export, FrameStrings.exportPdf(), FrameIconsExtra.ImageFormat, onPdf))
            onContactSheet?.let { add(MoreAction(MoreGroup.Export, FrameStrings.exportContact(), com.frameout.designsystem.icons.FrameIconsExtra.ContactSheet, it)) }
            add(MoreAction(MoreGroup.Export, FrameStrings.exportZip(), FrameIconsExtra.Storage, onZip))
            add(MoreAction(MoreGroup.Export, FrameStrings.exportCsv(), FrameIconsExtra.Storage, onCsv))
            add(MoreAction(MoreGroup.Export, FrameStrings.exportEvidence(), FrameIconsExtra.Warning, onEvidence))
        }
        add(MoreAction(MoreGroup.Danger, FrameStrings.delete(), FrameIcons.Trash, onDelete))
    }
}

@Composable
internal fun MoreSheet(visible: Boolean, selected: Int, actions: List<MoreAction>, onClose: () -> Unit) {
    fun of(group: MoreGroup) = actions.filter { it.group == group }
    FramesSheet(visible, FrameStrings.frameCount(selected), onClose) {
        val tiles = listOf(of(MoreGroup.Transform), of(MoreGroup.Edit)).filter { it.isNotEmpty() }
        val onlyRows = tiles.size == 1 && of(MoreGroup.Transform).isEmpty()
        if (tiles.isNotEmpty()) {
            SettingsSection(FrameStrings.groupEdit()) {
                SettingsCard {
                    tiles.forEachIndexed { i, row ->
                        if (i > 0) SettingsDivider(inset = 0.dp)
                        if (onlyRows) row.forEach { NavRow(it.icon, it.label, onClick = { onClose(); it.onClick() }) } else TileRow(row, onClose)
                    }
                }
            }
        }
        RowsSection(FrameStrings.groupShare(), of(MoreGroup.Share), onClose)
        RowsSection(FrameStrings.groupExport(), of(MoreGroup.Export), onClose)
        val danger = of(MoreGroup.Danger)
        if (danger.isNotEmpty()) {
            SettingsSection(FrameStrings.delete()) {
                SettingsCard { danger.forEachIndexed { i, a -> if (i > 0) SettingsDivider(); DangerRow(a, onClose) } }
            }
        }
    }
}

@Composable
private fun RowsSection(title: String, items: List<MoreAction>, onClose: () -> Unit) {
    if (items.isEmpty()) return
    SettingsSection(title) {
        SettingsCard {
            items.forEachIndexed { i, a ->
                if (i > 0) SettingsDivider()
                NavRow(a.icon, a.label, onClick = { onClose(); a.onClick() })
            }
        }
    }
}

@Composable
private fun TileRow(items: List<MoreAction>, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { a ->
            Column(
                Modifier.weight(1f).clip(FrameShapes.md).frameClickable { onClose(); a.onClick() }.padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SettingsIconTile(a.icon)
                FrameText(a.label, style = FrameTheme.typography.label, color = FrameTheme.colors.onSurface, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

@Composable
private fun DangerRow(action: MoreAction, onClose: () -> Unit) {
    val colors = FrameTheme.colors
    Row(
        Modifier.fillMaxWidth().frameClickable { onClose(); action.onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(36.dp).clip(FrameShapes.sm).background(colors.error.copy(alpha = 0.12f)), Alignment.Center) {
            Image(painterResource(action.icon), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(colors.error))
        }
        FrameText(action.label, style = rowTitleStyle(), color = colors.error)
    }
}
