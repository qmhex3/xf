package com.frameout.core.quality

import com.frameout.core.db.FrameDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object FrameSharpnessAnalyzer {
    private const val ID_CHUNK = 500

    suspend fun scoreProject(dao: FrameDao, projectId: Long): List<ScoredFrame> {
        val profiles = ArrayList<MotionEstimate.Profile?>()
        val scored = FrameScan.mapThumbnails(dao, projectId) { frame, bitmap ->
            val luma = Sharpness.lumaOf(bitmap)
            val focus = FocusScore.detail(luma, bitmap.width, bitmap.height)
            profiles += MotionEstimate.profile(luma, bitmap.width, bitmap.height)
            ScoredFrame(
                frame.id, frame.index, focus.score, ptsUs = frame.ptsUs,
                sig = FrameSignature.of(luma, bitmap.width, bitmap.height),
                noise = FrameSignature.cellNoise(focus.noise, bitmap.width, bitmap.height)
            )
        }
        val speeds = MotionEstimate.speeds(profiles)
        return scored.mapIndexed { i, f -> f.copy(speed = speeds[i]) }
    }

    suspend fun bestIds(dao: FrameDao, projectId: Long): List<Long> {
        val scored = scoreProject(dao, projectId)
        if (scored.isEmpty()) return emptyList()
        val candidateIds = SharpestPicker.candidates(scored)
        val faces = faceScores(dao, candidateIds)
        val pool = scored.map { f ->
            when {
                f.id !in candidateIds -> f.copy(score = -1.0)
                else -> faces[f.id]?.let { f.copy(faceScore = it) } ?: f
            }
        }
        return SharpestPicker.pick(pool)
    }

    private suspend fun faceScores(dao: FrameDao, ids: Set<Long>): Map<Long, Float> = withContext(Dispatchers.IO) {
        val out = HashMap<Long, Float>()
        ids.toList().chunked(ID_CHUNK).forEach { chunk ->
            dao.getByIds(chunk).forEach { frame ->
                ensureActive()
                val assessment = FaceScanner.assessFrame(frame)
                if (assessment.level != FaceLevel.None) out[frame.id] = assessment.score
            }
        }
        out
    }
}
