package com.frameout.app.frames

import com.frameout.core.util.FrameStrings

private val FAILURE_WORDS = listOf("fail", "couldn't", "unable", "فشل", "تعذّر", "تعذر", "لا توجد مساحة", "başarısız", "yapılamadı", "edilemedi", "не удалось", "недостаточно места", "нет места", "échec", "impossible", "pas pu", "insuffisant", "introuvable", "विफल", "नहीं हो सका", "नहीं हो सकी", "नहीं हो सके", "नहीं मिला", "नहीं खोल", "पर्याप्त जगह नहीं", "पर्याप्त नहीं", "失敗", "できませんでした", "できません", "見つかりません", "空き容量が足りません", "容量が足りません", "失败", "无法", "找不到", "空间不足", "存储空间不足")

private fun failureTexts(): Set<String> = setOf(
    FrameStrings.deleteFailed(),
    FrameStrings.noSpace(),
    FrameStrings.saveFailed(),
    FrameStrings.reassembleFailed(),
    FrameStrings.savedMissing(),
    FrameStrings.noSourceFrame(),
    FrameStrings.sourceGone(),
    FrameStrings.nothingSaved(),
    FrameStrings.exportSourceMissing(),
    FrameStrings.gifNeedsTwo(),
    FrameStrings.compareNeedsTwo(),
    FrameStrings.contactNeedsOne(),
    FrameStrings.contactTooMany(),
    FrameStrings.backupInvalid(),
    FrameStrings.backupNewer()
)

internal fun isStatusError(message: String): Boolean =
    FAILURE_WORDS.any { message.contains(it, ignoreCase = true) } || message in failureTexts()
