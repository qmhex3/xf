package com.frameout.core.util

import com.frameout.core.R

object SavedStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun deleted() = s(R.string.saved_deleted)
    fun restoreFailed() = s(R.string.saved_restore_failed)
    fun deleteFailed() = s(R.string.saved_delete_failed)
    fun restored(count: Int) = StringSource.plural(R.plurals.saved_restored_n, count, count)
    fun deletedN(count: Int) = StringSource.plural(R.plurals.saved_deleted_n, count, count)
    fun pruned(count: Int) = StringSource.plural(R.plurals.saved_pruned_n, count, count)
    fun scrubSpeed(multiple: Int) = s(R.string.scrub_speed, multiple)
    fun galleryDone(count: Int) = s(R.string.saved_gallery_done, count)
    fun galleryThere(count: Int) = s(R.string.saved_gallery_there, count)
    fun galleryAllThere() = s(R.string.saved_gallery_all_there)
    fun galleryFailed(count: Int) = s(R.string.saved_gallery_failed, count)
    fun exportTitle() = s(R.string.saved_export_title)
    fun exportZipHint() = s(R.string.saved_export_zip_hint)
    fun exportFolder() = s(R.string.saved_export_folder)
    fun exportFolderHint() = s(R.string.saved_export_folder_hint)
    fun exportDone(count: Int) = StringSource.plural(R.plurals.saved_export_done_n, count, count)
    fun exportPartial(done: Int, total: Int) = s(R.string.saved_export_partial, done, total)
    fun saveToGallery() = s(R.string.save_to_gallery)
    fun exportPdfHint() = s(R.string.saved_export_pdf_hint)
    fun exportContact() = s(R.string.saved_export_contact)
    fun exportContactHint() = s(R.string.saved_export_contact_hint)
    fun exportContactSheetHint() = s(R.string.saved_export_contact_hint_sheet)
    fun exportNoneReadable() = s(R.string.saved_export_none_readable)
}
