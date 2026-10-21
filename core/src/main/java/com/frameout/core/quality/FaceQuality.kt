package com.frameout.core.quality

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

enum class FaceLevel { None, Good, Fair, Poor }

data class FaceMark(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val leftEyeOpen: Float? = null,
    val rightEyeOpen: Float? = null,
    val smile: Float? = null,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val eyesOpen: Float get() = min(leftEyeOpen ?: UNKNOWN, rightEyeOpen ?: UNKNOWN)

    companion object {
        const val UNKNOWN = 0.6f
    }
}

data class FaceBox(val box: NormBox, val smile: Float, val eyesOpen: Float)

data class FaceAssessment(
    val faces: Int,
    val eyesOpen: Float,
    val sharp: Float,
    val size: Float,
    val frontal: Float,
    val smile: Float,
    val score: Float,
    val level: FaceLevel,
    val boxes: List<FaceBox> = emptyList(),
    val eyesKnown: Boolean = true
) {
    val smiling: Boolean get() = smile >= FaceQuality.SMILE_THRESHOLD

    companion object {
        val NONE = FaceAssessment(0, 0f, 0f, 0f, 0f, 0f, 0f, FaceLevel.None)
    }
}

object FaceQuality {
    const val SMILE_THRESHOLD = 0.6f
    private const val SIZE_FULL = 0.30f
    private const val YAW_LIMIT = 45f
    private const val PITCH_LIMIT = 35f
    private const val ROLL_LIMIT = 50f
    const val RELIABLE_PX = 64
    private const val EYE_YAW_LIMIT = 30f
    private const val EYE_PITCH_LIMIT = 25f
    private const val SQUINT_SMILE = 0.8f
    private const val SQUINT_MIN = 0.2f
    private const val SQUINT_MAX = 0.4f
    private const val SQUINT_VALUE = 0.5f
    private const val UNKNOWN_SHARP = 0.5f

    fun reliable(mark: FaceMark): Boolean = min(mark.width, mark.height) >= RELIABLE_PX

    fun eyeOpenness(mark: FaceMark): Float? {
        if (!reliable(mark)) return null
        if (abs(mark.yaw) > EYE_YAW_LIMIT || abs(mark.pitch) > EYE_PITCH_LIMIT) return null
        val open = listOfNotNull(mark.leftEyeOpen, mark.rightEyeOpen).minOrNull() ?: return null
        if ((mark.smile ?: 0f) >= SQUINT_SMILE && open >= SQUINT_MIN && open < SQUINT_MAX) return SQUINT_VALUE
        return open
    }

    fun faceSharpness(luma: IntArray, width: Int, height: Int, mark: FaceMark): Float {
        val x0 = mark.left.coerceIn(0, width - 1)
        val x1 = mark.right.coerceIn(x0 + 1, width)
        val y0 = mark.top.coerceIn(0, height - 1)
        val y1 = mark.bottom.coerceIn(y0 + 1, height)
        val cw = x1 - x0
        val ch = y1 - y0
        if (cw < 3 || ch < 3) return 0f
        val crop = IntArray(cw * ch)
        for (y in 0 until ch) System.arraycopy(luma, (y0 + y) * width + x0, crop, y * cw, cw)
        val variance = Sharpness.score(crop, cw, ch)
        return (log10(1.0 + variance) / 3.0).toFloat().coerceIn(0f, 1f)
    }

    fun sizeRatio(mark: FaceMark, width: Int, height: Int): Float {
        val base = min(width, height).toFloat()
        if (base <= 0f) return 0f
        return (min(mark.width, mark.height) / base / SIZE_FULL).coerceIn(0f, 1f)
    }

    fun frontalness(mark: FaceMark): Float {
        val yaw = abs(mark.yaw) / YAW_LIMIT
        val pitch = abs(mark.pitch) / PITCH_LIMIT
        val roll = abs(mark.roll) / ROLL_LIMIT
        return (1f - max(yaw, max(pitch, roll))).coerceIn(0f, 1f)
    }

    fun assess(luma: IntArray, width: Int, height: Int, marks: List<FaceMark>): FaceAssessment {
        if (marks.isEmpty() || width < 8 || height < 8 || luma.size < width * height) return FaceAssessment.NONE
        val largest = marks.maxOf { max(it.width, it.height) }
        val relevant = if (marks.size > 1) marks.filter { max(it.width, it.height) >= largest * 0.5f } else marks
        var sharp = 1f
        var frontal = 1f
        var size = 0f
        relevant.forEach { m ->
            sharp = min(sharp, if (reliable(m)) faceSharpness(luma, width, height, m) else UNKNOWN_SHARP)
            frontal = min(frontal, frontalness(m))
            size = max(size, sizeRatio(m, width, height))
        }
        val eyes = relevant.mapNotNull(::eyeOpenness)
        val open = eyes.minOrNull() ?: FaceMark.UNKNOWN
        val sure = relevant.filter(::reliable)
        val smile = if (sure.isEmpty()) 0f else sure.sumOf { (it.smile ?: 0f).toDouble() }.toFloat() / sure.size
        val score = (0.35f * open + 0.25f * sharp + 0.15f * size + 0.15f * frontal + 0.10f * smile).coerceIn(0f, 1f)
        return FaceAssessment(relevant.size, open, sharp, size, frontal, smile, score, levelOf(open, sharp, size, frontal), eyesKnown = eyes.isNotEmpty())
    }

    fun levelOf(open: Float, sharp: Float, size: Float, frontal: Float): FaceLevel = when {
        open < 0.4f || sharp < 0.25f || frontal < 0.2f -> FaceLevel.Poor
        open >= 0.7f && sharp >= 0.4f && size >= 0.3f && frontal >= 0.5f -> FaceLevel.Good
        else -> FaceLevel.Fair
    }
}
