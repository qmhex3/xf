package com.frameout.app.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings

private const val DEFAULT_PATH = "Pictures/FrameOut"

internal fun hasFolderAccess(context: Context, uri: String): Boolean {
    if (uri.isBlank()) return false
    val tree = Uri.parse(uri)
    return context.contentResolver.persistedUriPermissions.any { it.uri == tree && it.isWritePermission }
}

internal fun folderLabel(context: Context, uri: String): String? = runCatching {
    val tree = Uri.parse(uri)
    val document = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    context.contentResolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()

internal fun folderValue(context: Context, uri: String): String =
    if (hasFolderAccess(context, uri)) folderLabel(context, uri) ?: FrameStrings.customFolder() else FrameStrings.defaultFolder()

@Composable
internal fun rememberFolderPicker(prefs: AppPrefs, onPicked: (String) -> Unit = { prefs.setSaveFolderUri(it) }): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val granted = runCatching { context.contentResolver.takePersistableUriPermission(tree, flags) }.isSuccess
            if (granted) onPicked(tree.toString())
        }
    }
    return remember(launcher) { { launcher.launch(null) } }
}

@Composable
internal fun SaveFolderSheet(visible: Boolean, prefs: AppPrefs, uri: String, onPick: () -> Unit, onClose: () -> Unit) =
    FolderSheet(visible, FrameStrings.saveFolder(), DEFAULT_PATH, uri, { prefs.clearSaveFolderUri() }, onPick, onClose)

@Composable
internal fun ExportFolderSheet(visible: Boolean, prefs: AppPrefs, uri: String, followsText: String, onPick: () -> Unit, onClose: () -> Unit) =
    FolderSheet(visible, FrameStrings.exportFolder(), followsText, uri, { prefs.clearExportFolderUri() }, onPick, onClose)

@Composable
private fun FolderSheet(visible: Boolean, title: String, defaultText: String, uri: String, onClear: () -> Unit, onPick: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val custom = hasFolderAccess(context, uri)
    LaunchedEffect(uri) { if (uri.isNotBlank() && !custom) onClear() }
    BottomSheetFrame(visible, onClose, title) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), Arrangement.spacedBy(2.dp)) {
            ChoiceItem(ChoiceOption("default", FrameStrings.defaultFolder(), defaultText), !custom) {
                onClear()
                onClose()
            }
            if (custom) ChoiceItem(ChoiceOption("custom", folderLabel(context, uri) ?: FrameStrings.customFolder(), FrameStrings.customFolder()), true) { onClose() }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
            SettingsActionButton(if (custom) FrameStrings.changeFolder() else FrameStrings.chooseFolder()) {
                onClose()
                onPick()
            }
        }
    }
}
