package com.frameout.core.quality

import com.frameout.core.db.FrameDao

data class HashedFrame(val id: Long, val index: Int, val hash: Long, val score: Double, val vHash: Long = 0L, val mean: Int = 0, val sig: ByteArray? = null, val noise: Float = 0f)

object DuplicateFinder {
    const val MAX_DISTANCE = 6
    const val MAX_MEAN_DELTA = 12

    const val FACE_TIE = 0.05f

    fun duplicates(frames: List<HashedFrame>, maxDistance: Int = MAX_DISTANCE, faces: Map<Long, Float> = emptyMap()): List<Long> {
        val result = ArrayList<Long>()
        groups(frames.sortedBy { it.index }, maxDistance).forEach { group ->
            val keep = keeper(group, faces)
            group.forEach { if (it.id != keep.id) result += it.id }
        }
        return result
    }

    fun keeper(group: List<HashedFrame>, faces: Map<Long, Float>): HashedFrame {
        val withFace = group.filter { (faces[it.id] ?: 0f) > 0f }
        if (withFace.isEmpty()) return group.maxBy { it.score }
        val best = withFace.maxOf { faces.getValue(it.id) }
        return withFace.filter { faces.getValue(it.id) >= best - FACE_TIE }.maxBy { it.score }
    }

    internal fun contested(frames: List<HashedFrame>, maxDistance: Int = MAX_DISTANCE): Set<Long> =
        groups(frames.sortedBy { it.index }, maxDistance).filter { it.size > 1 }.flatMap { g -> g.map { it.id } }.toSet()

    private fun similar(a: HashedFrame, b: HashedFrame, maxDistance: Int): Boolean {
        val sa = a.sig
        val sb = b.sig
        if (sa != null && sb != null) return FrameSignature.same(sa, sb, maxDistance.toDouble() / MAX_DISTANCE, a.noise, b.noise)
        return legacySimilar(a, b, maxDistance)
    }

    private fun legacySimilar(a: HashedFrame, b: HashedFrame, maxDistance: Int): Boolean =
        FrameHash.distance(a.hash, b.hash) <= maxDistance &&
            FrameHash.distance(a.vHash, b.vHash) <= maxDistance &&
            kotlin.math.abs(a.mean - b.mean) <= MAX_MEAN_DELTA

    private fun groups(sorted: List<HashedFrame>, maxDistance: Int): List<List<HashedFrame>> {
        val groups = ArrayList<MutableList<HashedFrame>>()
        for (frame in sorted) {
            val current = groups.lastOrNull()
            if (current != null && similar(current.first(), frame, maxDistance) && similar(current.last(), frame, maxDistance)) current += frame
            else groups += mutableListOf(frame)
        }
        return groups
    }

    suspend fun duplicateIds(dao: FrameDao, projectId: Long): List<Long> {
        val hashed = hashAll(dao, projectId)
        val ids = contested(hashed)
        if (ids.isEmpty()) return emptyList()
        val faces = HashMap<Long, Float>()
        FrameScan.mapThumbnails(dao, projectId, accept = { it.id in ids }) { frame, _ ->
            val assessment = FaceScanner.assessFrame(frame)
            if (assessment.level != FaceLevel.None) faces[frame.id] = assessment.score
            Unit
        }
        return duplicates(hashed, faces = faces)
    }

    private suspend fun hashAll(dao: FrameDao, projectId: Long): List<HashedFrame> =
        FrameScan.mapThumbnails(dao, projectId) { frame, bitmap ->
            val luma = Sharpness.lumaOf(bitmap)
            val focus = FocusScore.detail(luma, bitmap.width, bitmap.height)
            HashedFrame(
                frame.id, frame.index, FrameHash.dHash(luma, bitmap.width, bitmap.height), focus.score,
                FrameHash.vHash(luma, bitmap.width, bitmap.height), FrameHash.mean(luma, bitmap.width, bitmap.height),
                FrameSignature.of(luma, bitmap.width, bitmap.height), FrameSignature.cellNoise(focus.noise, bitmap.width, bitmap.height)
            )
        }
}
