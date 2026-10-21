package com.frameout.engine.extract

import android.graphics.Bitmap
import com.frameout.engine.index.SampleInfo

internal sealed class Staged(val sample: SampleInfo) {
    class Idle(sample: SampleInfo) : Staged(sample)
    class Frame(sample: SampleInfo, val pts: Long, val bitmap: Bitmap) : Staged(sample)
}
