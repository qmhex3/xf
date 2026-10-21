package com.frameout.engine.decode

import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class ColorParams(
    val limited: Boolean,
    val bt2020: Boolean,
    val transfer: Int,
    val peakNits: Float,
    val bt601: Boolean = false,
    val primaries: Int = -1,
    val rangeDeclared: Boolean = true,
    val matrix: Int = 0
) {
    fun withFullRange() = ColorParams(false, bt2020, transfer, peakNits, bt601, primaries, true, matrix)

    private val smpte240: Boolean get() = matrix == MATRIX_SMPTE240M && !bt2020

    val rv: Float get() = if (bt2020) 1.4746f else if (smpte240) 1.576f else if (bt601) 1.402f else 1.5748f
    val gu: Float get() = if (bt2020) 0.16455f else if (smpte240) 0.2266f else if (bt601) 0.344136f else 0.1873f
    val gv: Float get() = if (bt2020) 0.57135f else if (smpte240) 0.4766f else if (bt601) 0.714136f else 0.4681f
    val bu: Float get() = if (bt2020) 1.8814f else if (smpte240) 1.826f else if (bt601) 1.772f else 1.8556f

    val special: Int get() = if (matrix == MATRIX_YCGCO || matrix == MATRIX_BT2020_CL || matrix == MATRIX_ICTCP) matrix else 0

    val gamutClass: Int get() = if (primaries >= 0) primaries else if (bt2020) 1 else 0

    fun lumaTable(): FloatArray = FloatArray(256) { if (limited) (it - 16) / 219f else it / 255f }

    fun chromaTable(): FloatArray = FloatArray(256) { if (limited) (it - 128) / 224f else (it - 128) / 255f }

    val gamutConvert: Boolean get() = !hdr && gamutClass != 0

    val gamutMatrix: FloatArray get() = GamutConvert.matrixFor(gamutClass)

    val hdr: Boolean get() = transfer == MediaFormat.COLOR_TRANSFER_ST2084 || transfer == MediaFormat.COLOR_TRANSFER_HLG

    companion object {
        const val MATRIX_SMPTE240M = 7
        const val MATRIX_YCGCO = 8
        const val MATRIX_BT2020_CL = 10
        const val MATRIX_ICTCP = 14

        fun from(primary: MediaFormat?, secondary: MediaFormat? = null): ColorParams {
            val declaredFirst = containerDeclared(secondary)
            val transfer = resolveTransfer(primary, secondary, declaredFirst)
            val standard = colorInt(primary, secondary, MediaFormat.KEY_COLOR_STANDARD, declaredFirst)
            val hdrTransfer = transfer == MediaFormat.COLOR_TRANSFER_ST2084 || transfer == MediaFormat.COLOR_TRANSFER_HLG
            val explicit709 = standard == MediaFormat.COLOR_STANDARD_BT709 || standard == MediaFormat.COLOR_STANDARD_BT601_PAL || standard == MediaFormat.COLOR_STANDARD_BT601_NTSC
            val bt2020 = standard == MediaFormat.COLOR_STANDARD_BT2020 || (hdrTransfer && !explicit709)
            val jpeg = isJpegFamily(primary, secondary)
            val matrix = firstInt(primary, secondary, ColorHints.KEY_MATRIX, 0)
            val primariesClass = primariesClassOf(firstInt(primary, secondary, ColorHints.KEY_PRIMARIES, 0), bt2020, standard, hdrTransfer)
            val declaredRange = colorInt(primary, secondary, MediaFormat.KEY_COLOR_RANGE, declaredFirst)
            val limited = when (declaredRange) {
                MediaFormat.COLOR_RANGE_FULL -> false
                MediaFormat.COLOR_RANGE_LIMITED -> true
                else -> !jpeg
            }
            val trustedRange = jpeg || declaredRange == MediaFormat.COLOR_RANGE_FULL ||
                (declaredRange == MediaFormat.COLOR_RANGE_LIMITED && containerRange(secondary) != 0)
            return ColorParams(
                limited = limited,
                bt2020 = bt2020,
                transfer = transfer,
                peakNits = peak(transfer == MediaFormat.COLOR_TRANSFER_HLG, primary, secondary),
                rangeDeclared = trustedRange,
                primaries = primariesClass,
                matrix = matrix,
                bt601 = !bt2020 && matrix != MATRIX_SMPTE240M && (standard == MediaFormat.COLOR_STANDARD_BT601_PAL || standard == MediaFormat.COLOR_STANDARD_BT601_NTSC ||
                    (standard == 0 && jpeg) || (standard != MediaFormat.COLOR_STANDARD_BT709 && isSd(primary, secondary)))
            )
        }

        private fun primariesClassOf(code: Int, bt2020: Boolean, standard: Int, hdr: Boolean): Int = when (code) {
            ColorHints.PRIMARIES_BT709 -> 0
            ColorHints.PRIMARIES_BT2020 -> 1
            ColorHints.PRIMARIES_DISPLAY_P3 -> 2
            ColorHints.PRIMARIES_DCI_P3 -> 3
            else -> if (bt2020) 1 else if (standard == MediaFormat.COLOR_STANDARD_BT709) 0 else if (hdr) 1 else 0
        }

        private fun isJpegFamily(a: MediaFormat?, b: MediaFormat?): Boolean {
            val mime = when {
                a?.containsKey(MediaFormat.KEY_MIME) == true -> a.getString(MediaFormat.KEY_MIME)
                b?.containsKey(MediaFormat.KEY_MIME) == true -> b.getString(MediaFormat.KEY_MIME)
                else -> null
            }?.lowercase() ?: return false
            return mime.contains("jpeg") || mime.contains("jpg")
        }

        private fun isSd(a: MediaFormat?, b: MediaFormat?): Boolean {
            val w = firstInt(a, b, MediaFormat.KEY_WIDTH, 0)
            val h = firstInt(a, b, MediaFormat.KEY_HEIGHT, 0)
            return w > 0 && h > 0 && h <= 576 && w < 1280
        }

        private fun containerRange(input: MediaFormat?): Int =
            if (input?.containsKey(MediaFormat.KEY_COLOR_RANGE) == true) input.getInteger(MediaFormat.KEY_COLOR_RANGE) else 0

        private fun containerDeclared(input: MediaFormat?): Boolean = input?.containsKey(ColorHints.KEY_MATRIX) == true

        private fun colorInt(output: MediaFormat?, input: MediaFormat?, key: String, containerFirst: Boolean): Int =
            if (containerFirst) firstInt(input, output, key, 0) else firstInt(output, input, key, 0)

        private fun resolveTransfer(a: MediaFormat?, b: MediaFormat?, containerFirst: Boolean): Int {
            val declared = colorInt(a, b, MediaFormat.KEY_COLOR_TRANSFER, containerFirst)
            if (declared != 0) return declared
            if (staticInfo(a) != null || staticInfo(b) != null || hdrProfile(a) || hdrProfile(b)) return MediaFormat.COLOR_TRANSFER_ST2084
            if (firstInt(a, b, ColorHints.KEY_MATRIX, 0) == MATRIX_ICTCP) return MediaFormat.COLOR_TRANSFER_ST2084
            return MediaFormat.COLOR_TRANSFER_SDR_VIDEO
        }

        private fun hdrProfile(format: MediaFormat?): Boolean {
            if (format?.containsKey(MediaFormat.KEY_PROFILE) != true) return false
            return when (format.getInteger(MediaFormat.KEY_PROFILE)) {
                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10,
                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus,
                MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR,
                MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus,
                MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10,
                MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus -> true
                else -> false
            }
        }

        private fun firstInt(a: MediaFormat?, b: MediaFormat?, key: String, fallback: Int): Int = when {
            a?.containsKey(key) == true -> a.getInteger(key)
            b?.containsKey(key) == true -> b.getInteger(key)
            else -> fallback
        }

        private fun staticInfo(format: MediaFormat?): ByteBuffer? =
            if (format?.containsKey(MediaFormat.KEY_HDR_STATIC_INFO) == true) format.getByteBuffer(MediaFormat.KEY_HDR_STATIC_INFO) else null

        private fun peak(hlg: Boolean, a: MediaFormat?, b: MediaFormat?): Float {
            val info = (staticInfo(a) ?: staticInfo(b))?.duplicate()?.order(ByteOrder.LITTLE_ENDIAN) ?: return PeakNits.DEFAULT
            if (info.limit() < 23) return PeakNits.DEFAULT
            val maxContent = info.getShort(21).toInt() and 0xFFFF
            val maxMastering = info.getShort(17).toInt() and 0xFFFF
            return PeakNits.resolve(maxContent, maxMastering, hlg)
        }
    }
}
