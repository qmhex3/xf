package com.frameout.app.frames

import com.frameout.core.quality.FaceAssessment
import com.frameout.core.quality.FaceExplain
import com.frameout.core.quality.FaceFactor
import com.frameout.core.util.QualityStrings

private fun pct(v: Float) = (v * 100f).toInt().coerceIn(0, 100)

internal fun faceWhy(a: FaceAssessment): String {
    val reason = when (FaceExplain.factor(a)) {
        FaceFactor.AllGood -> QualityStrings.factorGood()
        FaceFactor.EyesClosed -> QualityStrings.factorEyesClosed()
        FaceFactor.Blurry -> QualityStrings.factorBlurry()
        FaceFactor.TurnedAway -> QualityStrings.factorTurned()
        FaceFactor.SmallFace -> QualityStrings.factorSmall()
        FaceFactor.HalfOpenEyes -> QualityStrings.factorHalfEyes()
        FaceFactor.EyesUnverified -> QualityStrings.factorUnverified()
    }
    val lines = ArrayList<String>()
    lines += reason
    lines += ""
    lines += if (a.eyesKnown) QualityStrings.whyEyesOpen(pct(a.eyesOpen)) else QualityStrings.whyEyesUnknown()
    lines += QualityStrings.whySharp(pct(a.sharp))
    lines += QualityStrings.whyFrontal(pct(a.frontal))
    lines += QualityStrings.whySize(pct(a.size))
    if (a.smile > 0f) lines += QualityStrings.whySmile(pct(a.smile))
    lines += QualityStrings.whyFaces(a.faces)
    return lines.joinToString("\n")
}
