package com.frameout.core.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.util.FrameStrings

object SaveFolderGuard {
    private const val NOTICE_GAP_MS = 15_000L
    @Volatile var onNotice: (String) -> Unit = {}
    @Volatile private var lastNoticeAt = 0L

    fun resolve(context: Context, treeUri: String?): String? {
        if (treeUri.isNullOrBlank()) return null
        if (usable(context, Uri.parse(treeUri))) return treeUri
        val prefs = AppPrefs(context)
        if (prefs.saveFolderUri.value == treeUri) prefs.clearSaveFolderUri()
        if (prefs.exportFolderUri.value == treeUri) prefs.clearExportFolderUri()
        notice()
        return null
    }

    fun fellBack() = notice()

    private fun usable(context: Context, tree: Uri): Boolean {
        val granted = context.contentResolver.persistedUriPermissions.any { it.uri == tree && it.isWritePermission }
        if (!granted) return false
        return runCatching {
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            context.contentResolver.query(root, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use { it.moveToFirst() } == true
        }.getOrDefault(false)
    }

    private fun notice() {
        val now = System.currentTimeMillis()
        if (now - lastNoticeAt < NOTICE_GAP_MS) return
        lastNoticeAt = now
        onNotice(FrameStrings.saveFolderUnavailable())
    }
}
