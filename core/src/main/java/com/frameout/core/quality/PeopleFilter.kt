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
    const val POSE_CONFIDENCE = 0.7f

    fun isPerson(faces: Int, confidentLandmarks: Int): Boolean =
        faces > 0 || confidentLandmarks >= TagRules.MIN_LANDMARKS
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
            if (faceCount != null && PeopleRule.isPerson(faceCount, 0)) return@withPermit true
            if (!includeBody) return@withPermit faceCount == null
            val landmarks = detect("people-pose") { pose.process(image).await().allPoseLandmarks.count { it.inFrameLikelihood >= PeopleRule.POSE_CONFIDENCE } }
            if (faceCount == null && landmarks == null) true else PeopleRule.isPerson(faceCount ?: 0, landmarks ?: 0)
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
