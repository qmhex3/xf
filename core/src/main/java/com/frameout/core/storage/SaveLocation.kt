package com.frameout.core.storage

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

object SaveLocation {
    private val forbidden = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    fun folderName(raw: String): String {
        val cleaned = raw.replace(forbidden, " ").replace(Regex("\\s+"), " ").trim().trim('.').take(60).trim()
        return cleaned.ifBlank { "video" }
    }

    fun join(base: String, subfolder: String): String {
        val root = base.trim('/').replace("..", "")
        return if (subfolder.isBlank()) root else root + "/" + folderName(subfolder)
    }

    fun treeParent(resolver: ContentResolver, tree: Uri, subfolder: String): Uri {
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        if (subfolder.isBlank()) return root
        val name = folderName(subfolder)
        return findChild(resolver, tree, root, name)
            ?: DocumentsContract.createDocument(resolver, root, DocumentsContract.Document.MIME_TYPE_DIR, name)
            ?: root
    }

    private fun findChild(resolver: ContentResolver, tree: Uri, parent: Uri, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
        resolver.query(children, columns, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val isDir = c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                if (isDir && c.getString(1) == name) return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
            }
        }
        return null
    }
}
