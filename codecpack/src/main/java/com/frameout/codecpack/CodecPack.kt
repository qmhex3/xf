package com.frameout.codecpack

import android.content.Context
import android.os.Build
import java.io.File

object CodecPack {
    internal val SUPPORTED_ABIS = setOf("arm64-v8a", "armeabi-v7a", "x86_64")
    internal const val WRAPPER = "libframeout_ffmpeg.so"
    internal const val DIR = "codecpack"
    internal const val VERIFIED = ".verified"
    internal const val SERIAL_FILE = ".serial"
    internal const val MIN_SERIAL = 1L
    internal val REVOKED_SERIALS: Set<Long> = emptySet()
    internal const val SIGNATURE = "SIGNATURE.bin"
    internal const val PAYLOAD = "SIGNING_PAYLOAD.txt"
    internal const val MAX_PACK_BYTES = 48L * 1024L * 1024L
    internal const val MAX_TOTAL_UNCOMPRESSED = 64L * 1024L * 1024L
    internal const val MAX_ENTRY_BYTES = 10L * 1024L * 1024L
    internal const val MAX_ENTRIES = 64
    internal val dependencies = arrayOf("libavutil.so", "libswresample.so", "libswscale.so", "libavcodec.so", "libavformat.so")

    fun packDir(context: Context): File = File(context.filesDir, DIR).also { it.mkdirs() }
    fun abi(context: Context): String? = Build.SUPPORTED_ABIS.firstOrNull { it in SUPPORTED_ABIS }
    fun libraryFile(context: Context): File? = abi(context)?.let { File(abiDir(context), WRAPPER) }

    fun abiDir(context: Context): File {
        val selected = abi(context) ?: error("Unsupported ABI")
        return File(packDir(context), selected).also { it.mkdirs() }
    }

    fun isBundled(context: Context): Boolean =
        File(context.applicationInfo.nativeLibraryDir, "libavcodec.so").isFile &&
            File(context.applicationInfo.nativeLibraryDir, WRAPPER).isFile

    fun isInstalled(context: Context): Boolean = isBundled(context) || CodecPackVerifier.isInstalled(context)

    internal fun isSerialBlocked(serial: Long): Boolean = serial < MIN_SERIAL || serial in REVOKED_SERIALS

    internal fun installedSerial(context: Context): Long =
        runCatching { File(abiDir(context), SERIAL_FILE).readText().trim().toLong() }.getOrDefault(0L)

    fun installDetailed(context: Context, source: File): InstallResult {
        if (!source.isFile || source.length() <= 0L) return InstallResult(false, InstallError.Missing)
        if (source.length() > MAX_PACK_BYTES) return InstallResult(false, InstallError.TooLarge)
        return try {
            if (CodecPackInstaller.install(context, source)) InstallResult(true, null)
            else InstallResult(false, InstallError.Verify)
        } catch (e: InstallException) {
            InstallResult(false, e.error)
        } catch (e: Exception) {
            InstallResult(false, InstallError.Failed)
        }
    }

    fun load(context: Context): Boolean {
        if (FfmpegBridge.isAvailable) return true
        if (isBundled(context)) return FfmpegBridge.tryLoad()
        val dir = runCatching { abiDir(context) }.getOrNull() ?: return false
        val wrapper = libraryFile(context) ?: return false
        if (!isInstalled(context) || !CodecPackVerifier.verifyInstalled(dir)) return false
        if (isSerialBlocked(installedSerial(context))) return false
        return runCatching {
            dependencies.forEach { System.load(File(dir, it).absolutePath) }
            FfmpegBridge.tryLoad(wrapper.absolutePath)
        }.getOrDefault(false)
    }

    fun status(context: Context): String = when {
        FfmpegBridge.isAvailable -> "ready"
        isBundled(context) -> "bundled:${FfmpegBridge.error ?: "not_loaded"}"
        isInstalled(context) && isSerialBlocked(installedSerial(context)) -> "outdated"
        isInstalled(context) -> "installed:${FfmpegBridge.error ?: "load_failed"}"
        else -> "missing"
    }

    enum class InstallError { Missing, TooLarge, Older, Signature, Verify, LoadFailed, Failed }

    class InstallException(val error: InstallError, message: String) : Exception(message)

    data class InstallResult(val ok: Boolean, val error: InstallError?)
}
