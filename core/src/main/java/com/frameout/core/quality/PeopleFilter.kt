package com.frameout.core.quality

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object PeopleRule {
    fun isPerson(faces: Int, humanPose: Boolean): Boolean = faces > 0 || humanPose
}

object PeopleFilter {
    private const val SAMPLE_SIDE = 480
    private const val MIN_SIDE = 48
    private val gate = Semaphore(1)

    private val faces by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.05f)
                .build()
        )
    }
    private val pose by lazy {
        PoseDetection.getClient(PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE).build())
    }

    suspend fun containsPerson(source: Bitmap, includeBody: Boolean = true): Boolean = gate.withPermit {
        if (source.isRecycled || source.width < MIN_SIDE || source.height < MIN_SIDE) return@withPermit true
        val scale = SAMPLE_SIDE.toFloat() / maxOf(source.width, source.height)
        val small = if (scale >= 1f) source else Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(MIN_SIDE), (source.height * scale).toInt().coerceAtLeast(MIN_SIDE), true)
        val argb = if (small.config == Bitmap.Config.ARGB_8888) small else small.copy(Bitmap.Config.ARGB_8888, false) ?: return@withPermit true
        try {
            val image = InputImage.fromBitmap(argb, 0)
            val faceCount = detect("people-face") { faces.process(image).await().size }
            if (faceCount != null && PeopleRule.isPerson(faceCount, false)) return@withPermit true
            if (!includeBody) return@withPermit faceCount == null
            val w = argb.width.toFloat()
            val h = argb.height.toFloat()
            val posed = detect("people-pose") {
                val points = pose.process(image).await().allPoseLandmarks.map { PoseLm(it.landmarkType, it.position.x / w, it.position.y / h, it.inFrameLikelihood) }
                PoseEvidence.humanShaped(points, PoseEvidence.STRONG_CONFIDENT)
            }
            if (faceCount == null && posed == null) true else PeopleRule.isPerson(faceCount ?: 0, posed ?: false)
        } finally {
            if (argb !== small) argb.recycle()
            if (small !== source) small.recycle()
        }
    }

    private suspend fun <T> detect(name: String, block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        com.frameout.core.util.DetectorHealth.failed(name, e)
        null
    }
}
