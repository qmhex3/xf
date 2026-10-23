package com.frameout.app.viewer

import com.frameout.core.db.FrameEntity
import com.frameout.core.db.FrameTagDao
import com.frameout.core.edit.DetectionMapper
import com.frameout.core.edit.FrameEdit
import com.frameout.core.quality.NormBox
import com.frameout.core.quality.NormBoxCodec
import com.frameout.core.quality.SceneReasoner
import com.frameout.core.quality.TagFamilies
import com.frameout.core.quality.TagHit
import com.frameout.core.quality.TagRules
import kotlin.math.roundToInt

internal object ViewerDetections {
    private val QUIET = setOf(TagRules.GROUP, TagRules.GOOD_FACE)

    private const val WINDOW = 4

    suspend fun load(dao: FrameTagDao, frame: FrameEntity, edit: FrameEdit): List<Detection> {
        val stored = runCatching { dao.boxedTagsForFrame(frame.id) }.getOrDefault(emptyList()).filterNot { TagFamilies.isFamily(it.tag) }
        val hits = stored.map { TagHit(it.tag, it.confidence, NormBoxCodec.decode(it.boxes)) }
        val around = runCatching { dao.tagsAround(frame.projectId, frame.id, frame.index - WINDOW, frame.index + WINDOW) }
            .getOrDefault(emptyList())
            .filterNot { TagFamilies.isFamily(it.tag) }
            .groupBy { it.frameId }.values
            .map { rows -> rows.map { TagHit(it.tag, it.confidence, NormBoxCodec.decode(it.boxes)) } }
        val refined = SceneReasoner.refine(hits, around).filter { it.tag !in QUIET && it.boxes.isNotEmpty() }
        return group(refined.flatMap { hit ->
            hit.boxes.mapNotNull { DetectionMapper.map(it, edit) }.map { it to hit.tag }
        })
    }

    fun group(pairs: List<Pair<NormBox, String>>): List<Detection> =
        pairs.groupBy { (box, _) -> listOf(box.l, box.t, box.r, box.b).map { (it * 100).roundToInt() } }
            .values
            .map { same -> Detection(same.first().first, same.map { it.second }.distinct()) }
            .sortedByDescending { it.box.width * it.box.height }
}
