package com.frameout.codecpack

import android.content.Context
import java.io.File
import java.util.zip.ZipFile

internal object CodecPackInstaller {
    @Synchronized
    fun install(context: Context, source: File): Boolean {
        val expected = CodecPack.abi(context) ?: return false
        cleanOrphans(CodecPack.packDir(context))
        val staged = ZipFile(source).use { zip -> stageAndSwap(context, zip, expected) } ?: return false
        val alreadyLoaded = FfmpegBridge.isAvailable
        val usable = CodecPackVerifier.verifyInstalled(staged.dir) && (alreadyLoaded || CodecPack.load(context))
        if (!usable) {
            staged.dir.deleteRecursively()
            val saved = staged.backup
            if (saved != null && saved.exists()) saved.renameTo(staged.dir)
            throw CodecPack.InstallException(CodecPack.InstallError.LoadFailed, "load")
        }
        staged.backup?.deleteRecursively()
        return true
    }

    private class Installed(val dir: File, val backup: File?)

    private fun stageAndSwap(context: Context, zip: ZipFile, expected: String): Installed? {
        if (!CodecPackVerifier.verifyArchive(zip)) throw CodecPack.InstallException(CodecPack.InstallError.Verify, "archive")
        if (!CodecPackVerifier.verifySignature(zip)) throw CodecPack.InstallException(CodecPack.InstallError.Signature, "signature")
        val payload = zip.getEntry(CodecPack.PAYLOAD)?.let { String(CodecPackVerifier.readLimited(it, zip), Charsets.UTF_8) } ?: return null
        val serial = CodecPackSerial.parse(payload)
        if (CodecPackSerial.rejection(serial, CodecPack.installedSerial(context), CodecPack.MIN_SERIAL, CodecPack.REVOKED_SERIALS) != null) {
            throw CodecPack.InstallException(CodecPack.InstallError.Older, "serial")
        }
        val names = CodecPack.dependencies.map { "$expected/$it" } + "$expected/${CodecPack.WRAPPER}"
        if (names.any { zip.getEntry(it) == null } || zip.getEntry("$expected/SHA256SUMS") == null) return null
        val packDir = CodecPack.packDir(context)
        val target = File(packDir, expected)
        val temp = File(packDir, ".staging-${expected}-${System.nanoTime()}").also { it.mkdirs() }
        try {
            if (!populateStaging(zip, expected, temp, payload, serial.toString())) return null
            val swapped = swapDirectories(temp, target) ?: return null
            return Installed(target, swapped.backup)
        } finally {
            temp.deleteRecursively()
        }
    }

    private fun populateStaging(zip: ZipFile, expected: String, temp: File, payload: String, serialText: String): Boolean {
        val files = CodecPack.dependencies + CodecPack.WRAPPER
        val copied = files.all { name -> copyEntry(zip, "$expected/$name", File(temp, name)) }
        if (!copied || !CodecPackVerifier.verifyChecksums(zip, expected, temp, files) || !stageLicenses(zip, temp, payload)) return false
        files.forEach { name -> finalizeFile(File(temp, name)) }
        val marker = File(temp, CodecPack.VERIFIED)
        marker.writeText(files.joinToString("\n") { "$it ${CodecPackVerifier.sha256(File(temp, it))}" })
        marker.setReadable(true, true)
        marker.setWritable(false, true)
        File(temp, CodecPack.SERIAL_FILE).writeText(serialText)
        return true
    }

    private class Swap(val backup: File?)

    private fun swapDirectories(temp: File, dir: File): Swap? {
        val backup = File(dir.parentFile, ".backup-${dir.name}-${System.nanoTime()}")
        val hadPrevious = dir.exists()
        if (hadPrevious && !dir.renameTo(backup)) return null
        if (temp.renameTo(dir)) return Swap(if (hadPrevious) backup else null)
        dir.deleteRecursively()
        if (hadPrevious) backup.renameTo(dir)
        return null
    }

    private fun cleanOrphans(packDir: File) {
        packDir.listFiles()?.forEach { entry ->
            if (entry.name.startsWith(".staging-") || entry.name.startsWith(".backup-")) entry.deleteRecursively()
        }
    }

    private fun copyEntry(zip: ZipFile, name: String, target: File): Boolean = runCatching {
        target.parentFile?.mkdirs()
        val entry = zip.getEntry(name) ?: return false
        zip.getInputStream(entry).use { input -> target.outputStream().use { output -> CodecPackVerifier.copyLimited(input, output) } }
        target.isFile
    }.getOrDefault(false)

    private fun stageLicenses(zip: ZipFile, temp: File, payload: String): Boolean {
        val copied = CodecPackLicenses.TEXT_FILES.all { name -> copyEntry(zip, name, File(temp, name)) }
        if (!copied || !CodecPackVerifier.verifyLicenseFiles(payload, temp)) return false
        File(temp, CodecPack.PAYLOAD).writeText(payload)
        (CodecPackLicenses.TEXT_FILES.toList() + CodecPack.PAYLOAD).forEach { name ->
            File(temp, name).also { it.setReadable(true, true); it.setWritable(false, true) }
        }
        return true
    }

    private fun finalizeFile(file: File) {
        file.setReadable(true, true)
        file.setWritable(false, true)
        file.setExecutable(true, false)
    }
}
