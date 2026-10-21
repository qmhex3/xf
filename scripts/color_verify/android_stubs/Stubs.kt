package android.media
import java.nio.ByteBuffer
class MediaFormat {
    private val m = HashMap<String, Any>()
    fun containsKey(k: String) = m.containsKey(k)
    fun setInteger(k: String, v: Int) { m[k] = v }
    fun getInteger(k: String): Int = m[k] as Int
    fun getInteger(k: String, d: Int): Int = (m[k] as Int?) ?: d
    fun setString(k: String, v: String) { m[k] = v }
    fun getString(k: String): String? = m[k] as String?
    fun setByteBuffer(k: String, v: ByteBuffer) { m[k] = v }
    fun getByteBuffer(k: String): ByteBuffer? = m[k] as ByteBuffer?
    companion object {
        const val COLOR_RANGE_FULL = 1; const val COLOR_RANGE_LIMITED = 2
        const val COLOR_STANDARD_BT709 = 1; const val COLOR_STANDARD_BT601_PAL = 2; const val COLOR_STANDARD_BT601_NTSC = 4; const val COLOR_STANDARD_BT2020 = 6
        const val COLOR_TRANSFER_LINEAR = 1; const val COLOR_TRANSFER_SDR_VIDEO = 3; const val COLOR_TRANSFER_ST2084 = 6; const val COLOR_TRANSFER_HLG = 7
        const val KEY_COLOR_RANGE = "color-range"; const val KEY_COLOR_STANDARD = "color-standard"; const val KEY_COLOR_TRANSFER = "color-transfer"
        const val KEY_HDR_STATIC_INFO = "hdr-static-info"; const val KEY_HEIGHT = "height"; const val KEY_WIDTH = "width"; const val KEY_MIME = "mime"; const val KEY_PROFILE = "profile"
        const val MIMETYPE_VIDEO_AVC = "video/avc"; const val MIMETYPE_VIDEO_HEVC = "video/hevc"
    }
}
class MediaCodecInfo {
    object CodecProfileLevel {
        const val AV1ProfileMain10HDR10 = 4096; const val AV1ProfileMain10HDR10Plus = 8192
        const val HEVCProfileMain10HDR10 = 4096; const val HEVCProfileMain10HDR10Plus = 8192
        const val VP9Profile2HDR = 4096; const val VP9Profile2HDR10Plus = 8192
    }
}
