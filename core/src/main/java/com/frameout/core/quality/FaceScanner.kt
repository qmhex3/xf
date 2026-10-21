package com.frameout.core.quality

import android.graphics.Bitmap
import android.util.LruCache
import com.frameout.core.db.FrameEntity
import com.frameout.core.util.ThumbCache
import com.frameout.core.util.logFailure
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FaceDetectionFailed : Exception("face detector failed")

object FaceScanner {
    private const val MIN_SIDE = 48
    private const val CACHE_SIZE = 6000
    const val THUMB_SIDE = 512
    const val FULL_SIDE = 768
    private val gate = com.frameout.core.util.AdaptiveGate(2) { com.frameout.core.util.DeviceLimits.decodes() }
    private val cache = LruCache<String, FaceAssessment>(CACHE_SIZE)
    @Volatile private var lastFailure: String? = null

    private val detector: FaceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .setMinFaceSize(0.08f)
                .build()
        )
    }

    suspend fun assess(key: String?, bitmap: Bitmap): FaceAssessment =
        try {
            assessChecked(key, bitmap)
        } catch (e: FaceDetectionFailed) {
            FaceAssessment.NONE
        }

    suspend fun assessChecked(key: String?, bitmap: Bitmap): FaceAssessment {
        if (key.isNullOrBlank()) return FaceAssessment.NONE
        cache.get(key)?.let { return it }
        val result = gate.withPermit { analyze(bitmap) } ?: throw FaceDetectionFailed()
        cache.put(key, result)
        return result
    }

    suspend fun assessFrame(frame: FrameEntity): FaceAssessment {
        val key = frame.thumbnailPath ?: frame.fullPath ?: return FaceAssessment.NONE
        cache.get(key)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            frame.fullPath?.takeIf { it != key }?.let { ThumbCache.decodePooled(it, FULL_SIDE) } ?: ThumbCache.decodePooled(key, THUMB_SIDE)
        } ?: return FaceAssessment.NONE
        return try {
            assess(key, bitmap)
        } finally {
            ThumbCache.release(bitmap)
        }
    }

    fun cached(key: String?): FaceAssessment? = if (key.isNullOrBlank()) null else cache.get(key)

    private suspend fun analyze(source: Bitmap): FaceAssessment? {
        if (source.isRecycled || source.width < MIN_SIDE || source.height < MIN_SIDE) return FaceAssessment.NONE
        val argb = if (source.config == Bitmap.Config.ARGB_8888) source else source.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        return try {
            val faces = detector.process(InputImage.fromBitmap(argb, 0)).await()
            if (faces.isEmpty()) FaceAssessment.NONE
            else FaceQuality.assess(Sharpness.lumaOf(argb), argb.width, argb.height, faces.map(::markOf))
                .copy(boxes = faces.mapNotNull { boxOf(it, argb.width, argb.height) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            report(e)
            null
        } finally {
            if (argb !== source) argb.recycle()
        }
    }

    private fun report(e: Exception) {
        val line = "${e.javaClass.simpleName}: ${e.message}"
        if (line == lastFailure) return
        lastFailure = line
        logFailure("FaceScanner", e)
    }

    private fun boxOf(face: Face, width: Int, height: Int): FaceBox? {
        val b = face.boundingBox
        val box = NormBox.fromPixels(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(), width, height) ?: return null
        val open = minOf(face.leftEyeOpenProbability ?: FaceMark.UNKNOWN, face.rightEyeOpenProbability ?: FaceMark.UNKNOWN)
        return FaceBox(box, face.smilingProbability ?: 0f, open)
    }

    private fun markOf(face: Face): FaceMark {
        val box = face.boundingBox
        return FaceMark(
            box.left, box.top, box.right, box.bottom,
            face.leftEyeOpenProbability, face.rightEyeOpenProbability, face.smilingProbability,
            face.headEulerAngleY, face.headEulerAngleX, face.headEulerAngleZ
        )
    }
}
