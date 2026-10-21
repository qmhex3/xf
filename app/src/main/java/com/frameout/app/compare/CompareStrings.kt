package com.frameout.app.compare

import com.frameout.core.util.FrameStrings

internal object CompareStrings {
    private fun t(ar: String, en: String) = if (FrameStrings.isAr()) ar else en
    fun stack() = t("عمودي", "Stack")
    fun side() = t("متجاور", "Side")
    fun slider() = t("منزلق", "Slider")
    fun swap() = t("تبديل", "Swap")
    fun fit() = t("ملاءمة", "Fit")
    fun zoomIn() = t("تكبير", "Zoom in")
    fun zoomOut() = t("تصغير", "Zoom out")
    fun resetZoom() = t("إعادة الحجم الأصلي", "Reset zoom")
    fun hint() = t("اقرص للتكبير · انقر مرتين", "Pinch to zoom · double-tap")
    fun loadFailed() = t("تعذّر تحميل الصورة", "Couldn't load image")
    fun divider() = t("مقبض المقارنة", "Comparison divider")
    fun diff() = t("الفرق", "Diff")
    fun diffBusy() = t("جارٍ حساب الفروقات…", "Computing differences…")
    fun link() = t("ربط التكبير", "Link zoom")
    fun unlink() = t("تكبير مستقل", "Independent zoom")
    fun diffPercent(p: Float) = t("اختلاف ", "Changed ") + String.format(java.util.Locale.US, "%.1f%%", p)
    fun actualSize() = t("١٠٠٪ بالبكسل", "Actual pixels")
}
