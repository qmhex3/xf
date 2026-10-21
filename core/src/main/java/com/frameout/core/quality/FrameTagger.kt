package com.frameout.core.quality

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object FrameTagger {
    const val VERSION = 2
    private const val MIN_SIDE = 48
    private const val LABEL_CONFIDENCE = 0.6f
    private const val OBJECT_CONFIDENCE = 0.4f
    private const val LANDMARK_CONFIDENCE = 0.7f
    private const val MAX_REGIONS = 5
    private const val MIN_REGION = 64
    private const val REGION_PAD = 0.08f
    private const val REGION_LABELS = 2
    private const val BODY_PAD = 0.06f
    private val gate = Semaphore(1)

    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(LABEL_CONFIDENCE).build())
    }
    private val objects by lazy {
        ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()
        )
    }
    private val text by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val barcodes by lazy {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build())
    }
    private val pose by lazy {
        PoseDetection.getClient(PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE).build())
    }

    const val DETECTORS = 6

    suspend fun tag(key: String, source: Bitmap): List<TagHit> = tagDetailed(key, source).hits

    suspend fun tagDetailed(key: String, source: Bitmap): TagOutcome = gate.withPermit {
        if (source.isRecycled || source.width < MIN_SIDE || source.height < MIN_SIDE) return@withPermit TagOutcome(emptyList())
        val argb = if (source.config == Bitmap.Config.ARGB_8888) source else source.copy(Bitmap.Config.ARGB_8888, false) ?: return@withPermit TagOutcome(emptyList())
        try {
            runDetectors(key, argb)
        } finally {
            if (argb !== source) argb.recycle()
        }
    }

    private suspend fun runDetectors(key: String, argb: Bitmap): TagOutcome {
        val image = InputImage.fromBitmap(argb, 0)
        val run = DetectorRun(DETECTORS)
        val w = argb.width
        val h = argb.height
        run.step("face") { TagRules.faceTags(FaceScanner.assessChecked(key, argb)) }
        run.step("pose") {
            val landmarks = pose.process(image).await().allPoseLandmarks.filter { it.inFrameLikelihood >= LANDMARK_CONFIDENCE }
            TagRules.poseTags(landmarks.size, bodyBox(landmarks.map { it.position.x to it.position.y }, w, h))
        }
        val personBoxes = run.hits.filter { it.tag == TagRules.FACE || it.tag == TagRules.BODY }.flatMap { it.boxes }
        run.step("labels") { labeler.process(image).await().map { TagRules.labelTag(it.text, it.confidence) } }
        run.step("objects") { objectHits(argb, image, w, h, personBoxes) }
        run.step("text") {
            val result = text.process(image).await()
            TagRules.textTags(result.text, result.textBlocks.mapNotNull { block -> block.boundingBox?.let { box(it, w, h) } })
        }
        run.step("barcodes") {
            barcodes.process(image).await().flatMap { code -> TagRules.barcodeTags(code.valueType, listOfNotNull(code.boundingBox?.let { box(it, w, h) })) }
        }
        return run.outcome { SceneReasoner.refine(it).let(TagRules::merge) }
    }

    private suspend fun objectHits(argb: Bitmap, image: InputImage, w: Int, h: Int, personBoxes: List<NormBox>): List<TagHit> {
        val out = ArrayList<TagHit>()
        val detected = objects.process(image).await()
        detected.forEach { obj ->
            val region = box(obj.boundingBox, w, h)
            obj.labels.filter { it.confidence >= OBJECT_CONFIDENCE }.forEach { out += TagRules.objectTag(it.text, it.confidence, listOfNotNull(region)) }
        }
        detected.sortedByDescending { it.boundingBox.width() * it.boundingBox.height() }.take(MAX_REGIONS).forEach { obj ->
            val region = box(obj.boundingBox, w, h)?.takeIf { it.width * w >= MIN_REGION && it.height * h >= MIN_REGION } ?: return@forEach
            if (SceneReasoner.touchesPerson(region, personBoxes)) return@forEach
            val crop = cropOf(argb, region.padded(REGION_PAD)) ?: return@forEach
            try {
                labeler.process(InputImage.fromBitmap(crop, 0)).await().take(REGION_LABELS).forEach { out += TagRules.labelTag(it.text, it.confidence, listOf(region)) }
            } finally {
                if (crop !== argb) crop.recycle()
            }
        }
        return out
    }

    private fun cropOf(source: Bitmap, box: NormBox): Bitmap? {
        val rect = CropRules.rect(box, source.width, source.height) ?: return null
        return Bitmap.createBitmap(source, rect.x, rect.y, rect.width, rect.height)
    }

    private fun box(rect: android.graphics.Rect, w: Int, h: Int): NormBox? =
        NormBox.fromPixels(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat(), w, h)

    private fun bodyBox(points: List<Pair<Float, Float>>, w: Int, h: Int): NormBox? {
        if (points.isEmpty()) return null
        val raw = NormBox.fromPixels(points.minOf { it.first }, points.minOf { it.second }, points.maxOf { it.first }, points.maxOf { it.second }, w, h)
        return raw?.padded(BODY_PAD)
    }
}
