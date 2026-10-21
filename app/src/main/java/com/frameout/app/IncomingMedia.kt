package com.frameout.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.frameout.core.backup.BackupFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext
import java.io.File

object IncomingMedia {
    private const val DIR = "incoming"
    private val SURFACED_FAILURES = setOf(
        com.frameout.codecpack.MediaFailure.Drm,
        com.frameout.codecpack.MediaFailure.SourceMissing,
        com.frameout.codecpack.MediaFailure.PermissionDenied,
        com.frameout.codecpack.MediaFailure.CorruptData,
        com.frameout.codecpack.MediaFailure.NotAnimated,
        com.frameout.codecpack.MediaFailure.UnsupportedContainer
    )
    private const val RESERVE_BYTES = 64L * 1024 * 1024
    private const val SPACE_CHECK_BYTES = 8L * 1024 * 1024

    fun urisFrom(intent: Intent): List<Uri> {
        val found = when (intent.action) {
            Intent.ACTION_VIEW -> listOfNotNull(intent.data)
            Intent.ACTION_SEND -> listOfNotNull(streamExtra(intent)) + clipUris(intent)
            Intent.ACTION_SEND_MULTIPLE -> streamList(intent) + clipUris(intent)
            else -> emptyList()
        }
        return found.distinct()
    }

    class Stabilized(val ready: List<Uri>, val failed: Int, val at: Long = System.currentTimeMillis(), val rejected: Int = 0)

    suspend fun stabilize(context: Context, uris: List<Uri>): Stabilized = withContext(Dispatchers.IO) {
        var failed = 0
        var rejected = 0
        val job = coroutineContext[Job]
        val active = { job?.isActive != false }
        val ready = uris.filter { isReadable(context, it).also { ok -> if (!ok) rejected++ } }.mapNotNull { uri ->
            coroutineContext.ensureActive()
            when (classify(context, uri)) {
                Verdict.Reject -> { rejected++; null }
                Verdict.Surface -> uri
                Verdict.Keep ->
                    if (isDurable(context, uri) || readsWithoutGrant(context, uri)) uri
                    else copyLocal(context, uri, active).also { if (it == null) failed++ }
            }
        }
        Stabilized(ready, failed, rejected = rejected)
    }

    private enum class Verdict { Keep, Surface, Reject }

    private fun classify(context: Context, uri: Uri): Verdict = runCatching {
        if (isMotionPhoto(context, uri)) return@runCatching Verdict.Keep
        val info = com.frameout.codecpack.FormatProbe.probe(context, uri)
        when {
            info.kind != com.frameout.codecpack.FormatKind.Unsupported -> Verdict.Keep
            info.failure in SURFACED_FAILURES -> Verdict.Surface
            else -> Verdict.Reject
        }
    }.getOrDefault(Verdict.Keep)

    private fun isMotionPhoto(context: Context, uri: Uri): Boolean {
        val name = describe(context, uri).first
        val mime = context.contentResolver.getType(uri).orEmpty()
        return com.frameout.core.media.MotionPhotoExtractor.isMotionCandidate(mime, name) &&
            com.frameout.core.media.MotionPhotoExtractor.isMotionPhotoQuick(context, uri)
    }

    fun isBackup(context: Context, uri: Uri): Boolean =
        BackupFormat.isBackupName(describe(context, uri).first) || BackupFormat.isBackupName(uri.lastPathSegment)

    internal fun sanitizeName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(80).ifBlank { "video" }

    private fun clipUris(intent: Intent): List<Uri> {
        val clip = intent.clipData ?: return emptyList()
        return (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
            .filter { it.scheme == "content" || it.scheme == "file" }
    }

    @Suppress("DEPRECATION")
    private fun streamExtra(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else intent.getParcelableExtra(Intent.EXTRA_STREAM)

    @Suppress("DEPRECATION")
    private fun streamList(intent: Intent): List<Uri> =
        (if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)).orEmpty()

    internal fun isForeignPath(path: String?, privateRoots: List<File>, ownDir: File): Boolean {
        if (path == null) return true
        val target = runCatching { File(path).canonicalFile }.getOrNull() ?: return true
        val own = runCatching { ownDir.canonicalFile }.getOrNull()
        if (own != null && target.path.startsWith(own.path + File.separator)) return false
        return privateRoots.any { root ->
            val canonical = runCatching { root.canonicalFile }.getOrNull() ?: return@any true
            target.path == canonical.path || target.path.startsWith(canonical.path + File.separator)
        }
    }

    private fun isReadable(context: Context, uri: Uri): Boolean {
        val roots = listOfNotNull(
            context.applicationInfo.dataDir.let(::File),
            context.applicationInfo.deviceProtectedDataDir?.let(::File),
            context.filesDir.parentFile
        )
        if (uri.scheme != "file") return true
        return !isForeignPath(uri.path, roots, File(context.filesDir, DIR))
    }

    internal fun canReadWithoutCopy(authority: String?, sdk: Int, video: Boolean, images: Boolean, legacy: Boolean): Boolean =
        authority == MediaStore.AUTHORITY && if (sdk >= 33) video || images else legacy

    private fun readsWithoutGrant(context: Context, uri: Uri): Boolean {
        fun held(name: String) = context.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED
        return canReadWithoutCopy(
            uri.authority, Build.VERSION.SDK_INT,
            held(Manifest.permission.READ_MEDIA_VIDEO), held(Manifest.permission.READ_MEDIA_IMAGES),
            held(Manifest.permission.READ_EXTERNAL_STORAGE)
        )
    }

    private fun isDurable(context: Context, uri: Uri): Boolean {
        if (uri.scheme == "file") return true
        return runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess
    }

    private fun copyLocal(context: Context, uri: Uri, active: () -> Boolean = { true }): Uri? {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val (name, size) = describe(context, uri)
        if (size > 0L && dir.usableSpace < size + RESERVE_BYTES) return null
        if (dir.usableSpace < RESERVE_BYTES) return null
        val target = File(dir, "${System.currentTimeMillis()}_${sanitizeName(name)}")
        return runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: error("no stream")
            input.use { source ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var sinceCheck = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        if (!active()) error("cancelled")
                        output.write(buffer, 0, read)
                        sinceCheck += read
                        if (sinceCheck >= SPACE_CHECK_BYTES) {
                            sinceCheck = 0L
                            if (dir.usableSpace < RESERVE_BYTES) error("not enough space")
                        }
                    }
                }
            }
            Uri.fromFile(target)
        }.getOrElse { target.delete(); null }
    }

    fun releaseStaleGrants(context: Context, referencedUris: List<String>, keep: String = "", maxAgeMs: Long = 72L * 60L * 60L * 1000L) {
        val resolver = context.contentResolver
        val used = (referencedUris + keep).toSet()
        val cutoff = System.currentTimeMillis() - maxAgeMs
        resolver.persistedUriPermissions
            .filter { it.isReadPermission && !it.isWritePermission && it.uri.toString() !in used && it.persistedTime in 1 until cutoff }
            .forEach { runCatching { resolver.releasePersistableUriPermission(it.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
    }

    fun discardSource(context: Context, sourceUri: String, stillReferenced: List<String>) {
        if (sourceUri.isBlank() || sourceUri in stillReferenced) return
        val uri = runCatching { Uri.parse(sourceUri) }.getOrNull() ?: return
        if (uri.scheme == "file") {
            val file = runCatching { File(uri.path ?: return).canonicalFile }.getOrNull() ?: return
            val dir = runCatching { File(context.filesDir, DIR).canonicalFile }.getOrNull() ?: return
            if (file.path.startsWith(dir.path + File.separator)) file.delete()
        } else {
            runCatching { context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    fun cleanup(context: Context, referencedUris: List<String>, keep: String = "", maxAgeMs: Long = 72L * 60L * 60L * 1000L) {
        val dir = File(context.filesDir, DIR)
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        val referenced = (referencedUris + keep).mapNotNull { value ->
            runCatching { Uri.parse(value).takeIf { it.scheme == "file" }?.path }.getOrNull()
        }.map { runCatching { File(it).canonicalPath }.getOrDefault(it) }.toSet()
        val cutoff = System.currentTimeMillis() - maxAgeMs
        files.forEach { file ->
            val path = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
            if (path !in referenced && file.lastModified() < cutoff) file.delete()
        }
    }

    private fun describe(context: Context, uri: Uri): Pair<String, Long> {
        val columns = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val cursor = runCatching { context.contentResolver.query(uri, columns, null, null, null) }.getOrNull()
        cursor?.use { if (it.moveToFirst()) return (it.getString(0) ?: "video") to it.getLong(1) }
        return "video" to 0L
    }
}
