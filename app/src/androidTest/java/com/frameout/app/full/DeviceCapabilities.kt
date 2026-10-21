package com.frameout.app.full

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.frameout.codecpack.FfmpegBridge
import org.json.JSONArray
import org.json.JSONObject

object DeviceCapabilities {
    val mimes = listOf("video/avc", "video/hevc", "video/x-vnd.on2.vp8", "video/x-vnd.on2.vp9", "video/av01", "video/mpeg2", "video/mp4v-es", "video/3gpp")

    private fun infos(): List<MediaCodecInfo> = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.toList()

    private fun supports(info: MediaCodecInfo, mime: String): Boolean = info.supportedTypes.any { it.equals(mime, true) }

    fun hasEncoder(mime: String): Boolean = infos().any { it.isEncoder && supports(it, mime) }

    fun hasDecoder(mime: String): Boolean = infos().any { !it.isEncoder && supports(it, mime) }

    fun hasHardwareDecoder(mime: String): Boolean =
        Build.VERSION.SDK_INT >= 29 && infos().any { !it.isEncoder && supports(it, mime) && it.isHardwareAccelerated }

    fun encoderSupportsSize(mime: String, width: Int, height: Int): Boolean = infos().any {
        it.isEncoder && supports(it, mime) && runCatching {
            it.getCapabilitiesForType(mime).videoCapabilities?.isSizeSupported(width, height) == true
        }.getOrDefault(false)
    }

    fun ffmpegLoaded(): Boolean = FfmpegBridge.isAvailable || FfmpegBridge.tryLoad()

    fun freeBytes(context: Context): Long = runCatching { StatFs(context.filesDir.path).availableBytes }.getOrDefault(0L)

    fun totalMemoryBytes(context: Context): Long {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return android.app.ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }.totalMem
    }

    fun snapshot(context: Context): JSONObject = JSONObject().apply {
        put("api", Build.VERSION.SDK_INT)
        put("model", Build.MODEL)
        put("manufacturer", Build.MANUFACTURER)
        put("abis", JSONArray(Build.SUPPORTED_ABIS.toList()))
        put("cores", Runtime.getRuntime().availableProcessors())
        put("totalMemoryMb", totalMemoryBytes(context) / MB)
        put("freeStorageMb", freeBytes(context) / MB)
        put("ffmpeg", ffmpegLoaded())
        put("emulator", Build.FINGERPRINT.contains("generic") || Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"))
        put("codecs", codecs())
        put("externalStorage", Environment.getExternalStorageState())
    }

    private fun codecs(): JSONObject = JSONObject().also { out ->
        mimes.forEach { mime ->
            out.put(mime, JSONObject().put("encoder", hasEncoder(mime)).put("decoder", hasDecoder(mime)).put("hardwareDecoder", hasHardwareDecoder(mime)))
        }
    }

    private const val MB = 1024L * 1024L
}
