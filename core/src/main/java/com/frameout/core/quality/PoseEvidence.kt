package com.frameout.core.quality

import kotlin.math.hypot

class PoseLm(val type: Int, val x: Float, val y: Float, val likelihood: Float)

object PoseEvidence {
    const val LANDMARK_MIN = 0.80f
    const val MIN_CONFIDENT = 12
    const val STRONG_CONFIDENT = 20
    const val LANDMARKS_TOTAL = 33f

    private const val LEFT_SHOULDER = 11
    private const val RIGHT_SHOULDER = 12
    private const val LEFT_HIP = 23
    private const val RIGHT_HIP = 24
    private const val MIN_TORSO_RATIO = 0.35f
    private const val MAX_TORSO_RATIO = 6f

    fun confident(landmarks: List<PoseLm>): List<PoseLm> = landmarks.filter { it.likelihood >= LANDMARK_MIN }

    fun humanShaped(landmarks: List<PoseLm>, minConfident: Int = MIN_CONFIDENT): Boolean {
        val sure = confident(landmarks)
        if (sure.size < minConfident) return false
        val byType = sure.associateBy { it.type }
        val left = byType[LEFT_SHOULDER] ?: return false
        val right = byType[RIGHT_SHOULDER] ?: return false
        val hip = byType[LEFT_HIP] ?: byType[RIGHT_HIP] ?: return false
        val shoulders = hypot(left.x - right.x, left.y - right.y)
        if (shoulders <= 0f) return false
        val torso = hypot(hip.x - (left.x + right.x) / 2f, hip.y - (left.y + right.y) / 2f)
        return torso / shoulders in MIN_TORSO_RATIO..MAX_TORSO_RATIO
    }
}
