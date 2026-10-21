package com.frameout.engine.extract

import com.frameout.engine.decode.DecodeRouter
import kotlinx.coroutines.flow.Flow

internal class FfmpegSession(
    val router: DecodeRouter,
    val index: StagedIndex,
    val select: (Int) -> Flow<Int>,
    val count: suspend (Int) -> Int,
    val startUs: Long,
    val endUs: Long,
    val checkpoint: Long,
    val progressTotal: Int,
    var done: Int,
    var lastPts: Long,
    var lastIndex: Int,
    val startIndex: Int,
    val durationUs: Long
)

internal sealed class FfmpegStaged(val index: Int) {
    class Idle(index: Int, val histogram: FloatArray?, val tracked: Boolean) : FfmpegStaged(index)
    class Frame(index: Int, val pts: Long, val bitmap: android.graphics.Bitmap, val keyframe: Boolean, val histogram: FloatArray?) : FfmpegStaged(index)
}
