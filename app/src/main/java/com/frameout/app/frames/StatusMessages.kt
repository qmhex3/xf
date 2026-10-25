package com.frameout.app.frames

import com.frameout.core.util.FrameStrings

private val FAILURE_WORDS = listOf("fail", "couldn't", "unable", "فشل", "تعذّر", "تعذر", "لا توجد مساحة", "başarısız", "yapılamadı", "edilemedi", "не удалось", "недостаточно места", "нет места", "échec", "impossible", "pas pu", "insuffisant", "introuvable", "falha", "falhou", "não foi possível", "não é possível", "insuficiente", "não encontrad", "sem espaço", "विफल", "नहीं हो सका", "नहीं हो सकी", "नहीं हो सके", "नहीं मिला", "नहीं खोल", "पर्याप्त जगह नहीं", "पर्याप्त नहीं", "失敗", "できませんでした", "できません", "見つかりません", "空き容量が足りません", "容量が足りません", "失败", "无法", "找不到", "空间不足", "存储空间不足", "fehlgeschlagen", "konnte nicht", "konnten nicht", "nicht möglich", "nicht genug", "nicht genügend", "nicht gefunden", "nicht mehr vorhanden", "nicht verfügbar", "eșuat", "nu a putut", "nu au putut", "nu s-a putut", "nu este posibil", "nu se poate", "spațiu insuficient", "nu este suficient", "nu a fost găsit", "nu au fost găsit", "nu a fost găsită", "indisponibil", "error al", "no se pudo", "no se pudieron", "no es posible", "no se puede", "espacio insuficiente", "no hay espacio", "no hay suficiente", "no se encontr", "no encontrad", "ya no existe", "no disponible", "non riuscit", "non è stato possibile", "non è possibile", "impossibile", "non trovat", "non più disponibil", "spazio insufficiente", "spazio di archiviazione insufficiente", "memoria insufficiente", "mislyktes", "feilet", "kunne ikke", "ikke mulig", "ikke nok plass", "ikke nok lagring", "ikke funnet", "finnes ikke", "ikke lenger tilgjengelig", "utilstrekkelig", "gagal", "tidak dapat", "tidak boleh", "tidak mencukupi", "ruang tidak cukup", "tiada ruang", "tidak ditemui", "tidak dijumpai", "tidak lagi tersedia")

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
