package com.frameout.core.quality

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.LruCache
import com.frameout.core.db.FrameEntity
import com.frameout.core.util.DeviceLimits
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
import kotlin.math.roundToInt

class FaceDetectionFailed : Exception("face detector failed")

object FaceScanner {
    private const val MIN_SIDE = 48
    private const val CACHE_SIZE = 6000
    private const val MAX_REFINE = 12
    private const val DUPLICATE_IOU = 0.3f
    private const val DUPLICATE_CONTAIN = 0.6f
    const val THUMB_SIDE = 512
    private val gate = com.frameout.core.util.AdaptiveGate(2) { com.frameout.core.util.DeviceLimits.decodes() }
    private val cache = LruCache<String, FaceAssessment>(CACHE_SIZE)
    @Volatile private var lastFailure: String? = null

    private class Candidate(val rect: PxRect, val face: Face, val zoomed: Boolean, val fromTile: Boolean)

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

    private fun fullSide(): Int = AnalysisSource.side(DeviceLimits.analysisReduced())

    suspend fun assess(key: String?, bitmap: Bitmap): FaceAssessment =
        try {
            assessChecked(key, bitmap)
        } catch (e: FaceDetectionFailed) {
            FaceAssessment.NONE
        }

    suspend fun assessChecked(key: String?, bitmap: Bitmap): FaceAssessment = assessChecked(key, bitmap, null)

    internal suspend fun assessChecked(key: String?, bitmap: Bitmap, regions: FullResRegions?): FaceAssessment {
        if (key.isNullOrBlank()) return FaceAssessment.NONE
        cache.get(key)?.let { return it }
        val result = gate.withPermit { analyze(bitmap, regions) } ?: throw FaceDetectionFailed()
        cache.put(key, result)
        return result
    }

    suspend fun detect(bitmap: Bitmap): FaceAssessment? = gate.withPermit { analyze(bitmap, null) }

    suspend fun assessFrame(frame: FrameEntity): FaceAssessment {
        val key = frame.thumbnailPath ?: frame.fullPath ?: return FaceAssessment.NONE
        cache.get(key)?.let { return it }
        val full = frame.fullPath?.takeIf { it != key }
        val bitmap = withContext(Dispatchers.IO) {
            full?.let { ThumbCache.decodePooled(it, fullSide()) } ?: ThumbCache.decodePooled(key, THUMB_SIDE)
        } ?: return FaceAssessment.NONE
        val regions = full?.let { withContext(Dispatchers.IO) { FullResRegions.open(it) } }
        return try {
            val result = try {
                assessChecked(key, bitmap, regions)
            } catch (e: FaceDetectionFailed) {
                FaceAssessment.NONE
            }
            result
        } finally {
            regions?.close()
            ThumbCache.release(bitmap)
        }
    }

    fun cached(key: String?): FaceAssessment? = if (key.isNullOrBlank()) null else cache.get(key)

    private suspend fun analyze(source: Bitmap, regions: FullResRegions?): FaceAssessment? {
        if (source.isRecycled || source.width < MIN_SIDE || source.height < MIN_SIDE) return FaceAssessment.NONE
        val argb = if (source.config == Bitmap.Config.ARGB_8888) source else source.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        return try {
            val w = argb.width
            val h = argb.height
            val whole = faces(argb).mapNotNull { face -> rectOf(face.boundingBox, w, h)?.let { Candidate(it, face, false, false) } }
            var all = whole
            if (needsTiles(whole)) all = whole + extraTileFaces(argb, whole)
            all = lookCloser(argb, regions, all)
            if (all.isEmpty()) FaceAssessment.NONE
            else FaceQuality.assess(Sharpness.lumaOf(argb), w, h, all.map(::markOf))
                .copy(boxes = all.mapNotNull { boxOf(it, w, h) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            report(e)
            null
        } finally {
            if (argb !== source) argb.recycle()
        }
    }

    private suspend fun faces(bitmap: Bitmap): List<Face> = detector.process(InputImage.fromBitmap(bitmap, 0)).await()

    private fun needsTiles(whole: List<Candidate>): Boolean =
        !(whole.size >= TagRules.GROUP_SIZE && whole.none { it.rect.minSide < SmartZoom.DETECTABLE_PX })

    private suspend fun extraTileFaces(argb: Bitmap, whole: List<Candidate>): List<Candidate> {
        val found = ArrayList<Candidate>()
        try {
            ZoomScan.forEachTile(argb) { tile ->
                faces(tile.bitmap).forEach { face ->
                    val b = face.boundingBox
                    val rect = clamp(tile.toFrame(b.left, b.top, b.right, b.bottom), argb.width, argb.height)
                    if (SmartZoom.cutByTile(rect, tile.rect, argb.width, argb.height) || !SmartZoom.plausibleFace(rect)) return@forEach
                    found += Candidate(rect, face, minOf(b.width(), b.height()) >= SmartZoom.DETECTABLE_PX, true)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            report(e)
        }
        return SmartZoom.suppress(found, { it.rect }).filter { extra ->
            whole.none { SmartZoom.iou(it.rect, extra.rect) >= DUPLICATE_IOU || SmartZoom.containment(it.rect, extra.rect) >= DUPLICATE_CONTAIN }
        }
    }

    private suspend fun lookCloser(argb: Bitmap, regions: FullResRegions?, all: List<Candidate>): List<Candidate> {
        val targets = all.withIndex()
            .filter { (_, c) -> c.fromTile || c.rect.minSide < SmartZoom.DETECTABLE_PX }
            .sortedWith(compareByDescending<IndexedValue<Candidate>> { it.value.fromTile }.thenByDescending { it.value.rect.area })
            .take(MAX_REFINE)
        val refined = HashMap<Int, Candidate?>()
        for ((index, c) in targets) {
            refined[index] = try {
                closeUp(argb, regions, c)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                report(e)
                null
            }
        }
        return all.mapIndexedNotNull { i, c ->
            when {
                i in refined -> refined[i] ?: c.takeIf { !it.fromTile }
                c.fromTile -> c.takeIf { it.zoomed }
                else -> c
            }
        }
    }

    private suspend fun closeUp(argb: Bitmap, regions: FullResRegions?, c: Candidate): Candidate? {
        val w = argb.width
        val h = argb.height
        val window = SmartZoom.zoomRect(c.rect, w, h) ?: return null
        val crop = regions?.crop(window, w, h, SmartZoom.FACE_ZOOM_TARGET, SmartZoom.FACE_ZOOM_MAX_UPSCALE)
            ?: ZoomScan.crop(argb, window, SmartZoom.FACE_ZOOM_TARGET, SmartZoom.FACE_ZOOM_MAX_UPSCALE)
            ?: return null
        try {
            val sx = crop.bitmap.width.toFloat() / window.w
            val sy = crop.bitmap.height.toFloat() / window.h
            val expected = PxRect(
                ((c.rect.x - window.x) * sx).roundToInt(), ((c.rect.y - window.y) * sy).roundToInt(),
                (c.rect.w * sx).roundToInt().coerceAtLeast(1), (c.rect.h * sy).roundToInt().coerceAtLeast(1)
            )
            var best: Face? = null
            var bestIou = 0f
            for (f in faces(crop.bitmap)) {
                val b = f.boundingBox
                val v = SmartZoom.iou(PxRect(b.left, b.top, b.width(), b.height()), expected)
                if (v > bestIou) {
                    best = f
                    bestIou = v
                }
            }
            val face = best?.takeIf { bestIou >= SmartZoom.CONFIRM_IOU } ?: return null
            val b = face.boundingBox
            val back = PxRect(
                window.x + (b.left / sx).roundToInt(), window.y + (b.top / sy).roundToInt(),
                (b.width() / sx).roundToInt().coerceAtLeast(1), (b.height() / sy).roundToInt().coerceAtLeast(1)
            )
            return Candidate(clamp(back, w, h), face, true, c.fromTile)
        } finally {
            crop.release()
        }
    }

    private fun clamp(rect: PxRect, width: Int, height: Int): PxRect {
        val x = rect.x.coerceIn(0, width - 1)
        val y = rect.y.coerceIn(0, height - 1)
        return PxRect(x, y, rect.w.coerceIn(1, width - x), rect.h.coerceIn(1, height - y))
    }

    private fun rectOf(box: Rect, width: Int, height: Int): PxRect? {
        if (box.width() <= 0 || box.height() <= 0) return null
        return clamp(PxRect(box.left, box.top, box.width(), box.height()), width, height)
    }

    private fun report(e: Exception) {
        val line = "${e.javaClass.simpleName}: ${e.message}"
        if (line == lastFailure) return
        lastFailure = line
        logFailure("FaceScanner", e)
    }

    private fun boxOf(c: Candidate, width: Int, height: Int): FaceBox? {
        val r = c.rect
        val box = NormBox.fromPixels(r.x.toFloat(), r.y.toFloat(), r.r.toFloat(), r.b.toFloat(), width, height) ?: return null
        val open = minOf(c.face.leftEyeOpenProbability ?: FaceMark.UNKNOWN, c.face.rightEyeOpenProbability ?: FaceMark.UNKNOWN)
        return FaceBox(box, c.face.smilingProbability ?: 0f, open)
    }

    private fun markOf(c: Candidate): FaceMark {
        val face = c.face
        val r = c.rect
        return FaceMark(
            r.x, r.y, r.r, r.b,
            face.leftEyeOpenProbability, face.rightEyeOpenProbability, face.smilingProbability,
            face.headEulerAngleY, face.headEulerAngleX, face.headEulerAngleZ,
            c.zoomed
        )
    }
}
