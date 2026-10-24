package com.frameout.app.compare

import com.frameout.core.quality.TagText
import com.frameout.core.util.FrameStrings

internal object CompareStrings {
    private fun t(key: String, ar: String, en: String): String = when (val lang = FrameStrings.lang()) {
        "ar" -> ar
        "en" -> en
        else -> TagText.forLang(lang)?.compare?.get(key) ?: en
    }
    fun stack() = t("stack", "عمودي", "Stack")
    fun side() = t("side", "متجاور", "Side")
    fun slider() = t("slider", "منزلق", "Slider")
    fun swap() = t("swap", "تبديل", "Swap")
    fun fit() = t("fit", "ملاءمة", "Fit")
    fun zoomIn() = t("zoomIn", "تكبير", "Zoom in")
    fun zoomOut() = t("zoomOut", "تصغير", "Zoom out")
    fun resetZoom() = t("resetZoom", "إعادة الحجم الأصلي", "Reset zoom")
    fun hint() = t("hint", "اقرص للتكبير · انقر مرتين", "Pinch to zoom · double-tap")
    fun loadFailed() = t("loadFailed", "تعذّر تحميل الصورة", "Couldn't load image")
    fun divider() = t("divider", "مقبض المقارنة", "Comparison divider")
    fun diff() = t("diff", "الفرق", "Diff")
    fun diffBusy() = t("diffBusy", "جارٍ حساب الفروقات…", "Computing differences…")
    fun link() = t("link", "ربط التكبير", "Link zoom")
    fun unlink() = t("unlink", "تكبير مستقل", "Independent zoom")
    fun diffPercent(p: Float) = t("diffPercentPrefix", "اختلاف ", "Changed ") + String.format(java.util.Locale.US, "%.1f%%", p)
    fun actualSize() = t("actualSize", "١٠٠٪ بالبكسل", "Actual pixels")
}
