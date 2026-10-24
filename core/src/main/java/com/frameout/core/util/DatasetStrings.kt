package com.frameout.core.util

import com.frameout.core.R

object DatasetStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun title() = s(R.string.dataset_title)
    fun rowHint() = s(R.string.dataset_row_hint)
    fun hint() = s(R.string.dataset_hint)
    fun target() = s(R.string.dataset_target)
    fun colmap() = s(R.string.dataset_colmap)
    fun colmapHint() = s(R.string.dataset_colmap_hint)
    fun yolo() = s(R.string.dataset_yolo)
    fun yoloHint() = s(R.string.dataset_yolo_hint)
    fun coco() = s(R.string.dataset_coco)
    fun cocoHint() = s(R.string.dataset_coco_hint)
    fun interval() = s(R.string.dataset_interval)
    fun seconds(value: String) = s(R.string.dataset_seconds, value)
    fun blur() = s(R.string.dataset_blur)
    fun blurOff() = s(R.string.dataset_blur_off)
    fun blurNormal() = s(R.string.dataset_blur_normal)
    fun blurStrict() = s(R.string.dataset_blur_strict)
    fun duplicates() = s(R.string.dataset_duplicates)
    fun duplicatesHint() = s(R.string.dataset_duplicates_hint)
    fun rejected() = s(R.string.dataset_rejected)
    fun rejectedHint() = s(R.string.dataset_rejected_hint)
    fun create() = s(R.string.dataset_create)
    fun none() = s(R.string.dataset_none)
    fun saved(count: Int) = s(R.string.dataset_saved, count)
    fun scope() = s(R.string.dataset_scope)
}
