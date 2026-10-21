package com.frameout.core.quality

data class TagHit(val tag: String, val confidence: Float, val boxes: List<NormBox> = emptyList())

object TagRules {
    const val FACE = "p:face"
    const val GROUP = "p:group"
    const val SMILE = "p:smile"
    const val EYES_CLOSED = "p:eyes_closed"
    const val GOOD_FACE = "p:good_face"
    const val BODY = "p:body"
    const val TEXT = "t:text"
    const val CODE = "b:code"
    const val LABEL = "l:"
    const val OBJECT = "o:"
    const val MIN_LANDMARKS = 10
    const val MIN_TEXT_CHARS = 4
    const val GROUP_SIZE = 3

    fun faceTags(face: FaceAssessment): List<TagHit> {
        if (face.level == FaceLevel.None) return emptyList()
        val all = face.boxes.map { it.box }
        val out = ArrayList<TagHit>()
        out += TagHit(FACE, 1f, all)
        if (face.faces >= GROUP_SIZE) out += TagHit(GROUP, 1f, all)
        if (face.smiling) out += TagHit(SMILE, face.smile, face.boxes.filter { it.smile >= FaceQuality.SMILE_THRESHOLD }.map { it.box }.ifEmpty { all })
        if (face.eyesOpen < 0.4f) out += TagHit(EYES_CLOSED, 1f - face.eyesOpen, face.boxes.filter { it.eyesOpen < 0.4f }.map { it.box }.ifEmpty { all })
        if (face.level == FaceLevel.Good) out += TagHit(GOOD_FACE, face.score, all)
        return out
    }

    fun poseTags(confidentLandmarks: Int, box: NormBox? = null): List<TagHit> =
        if (confidentLandmarks >= MIN_LANDMARKS) listOf(TagHit(BODY, (confidentLandmarks / 33f).coerceIn(0f, 1f), listOfNotNull(box))) else emptyList()

    fun textTags(text: String, boxes: List<NormBox> = emptyList()): List<TagHit> =
        if (text.count { !it.isWhitespace() } >= MIN_TEXT_CHARS) listOf(TagHit(TEXT, 1f, boxes)) else emptyList()

    fun barcodeTags(valueType: Int, boxes: List<NormBox> = emptyList()): List<TagHit> {
        val out = arrayListOf(TagHit(CODE, 1f, boxes))
        barcodeKey(valueType)?.let { out += TagHit(it, 1f, boxes) }
        return out
    }

    fun labelTag(text: String, confidence: Float, boxes: List<NormBox> = emptyList()) = TagHit(LABEL + text.trim(), confidence, boxes)

    fun objectTag(text: String, confidence: Float, boxes: List<NormBox> = emptyList()) = TagHit(OBJECT + text.trim().lowercase().replace(' ', '_'), confidence, boxes)

    fun barcodeKey(valueType: Int): String? = when (valueType) {
        1 -> "b:contact"
        2 -> "b:email"
        3 -> "b:isbn"
        4 -> "b:phone"
        5 -> "b:product"
        6 -> "b:sms"
        7 -> "b:plain"
        8 -> "b:url"
        9 -> "b:wifi"
        10 -> "b:geo"
        11 -> "b:event"
        12 -> "b:license"
        else -> null
    }

    fun merge(hits: List<TagHit>): List<TagHit> =
        hits.groupBy { it.tag }.map { (tag, same) ->
            TagHit(tag, same.maxOf { it.confidence }, same.flatMap { it.boxes }.distinct().take(NormBoxCodec.MAX_PER_TAG))
        }
}
