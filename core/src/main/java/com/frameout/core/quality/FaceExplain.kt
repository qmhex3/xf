package com.frameout.core.quality

enum class FaceFactor { EyesClosed, Blurry, TurnedAway, SmallFace, HalfOpenEyes, EyesUnverified, AllGood }

object FaceExplain {
    private const val POOR_OPEN = 0.4f
    private const val POOR_SHARP = 0.25f
    private const val POOR_FRONTAL = 0.2f
    private const val GOOD_OPEN = 0.7f
    private const val GOOD_SHARP = 0.4f
    private const val GOOD_FRONTAL = 0.5f

    fun factor(a: FaceAssessment): FaceFactor = when (a.level) {
        FaceLevel.Good -> FaceFactor.AllGood
        FaceLevel.Poor -> when {
            a.eyesOpen < POOR_OPEN -> FaceFactor.EyesClosed
            a.sharp < POOR_SHARP -> FaceFactor.Blurry
            else -> FaceFactor.TurnedAway
        }
        else -> when {
            !a.eyesKnown -> FaceFactor.EyesUnverified
            a.eyesOpen < GOOD_OPEN -> FaceFactor.HalfOpenEyes
            a.sharp < GOOD_SHARP -> FaceFactor.Blurry
            a.frontal < GOOD_FRONTAL -> FaceFactor.TurnedAway
            else -> FaceFactor.SmallFace
        }
    }
}
