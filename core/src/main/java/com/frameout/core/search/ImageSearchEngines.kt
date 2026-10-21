package com.frameout.core.search

data class SearchEngine(
    val id: String,
    val name: String,
    val url: String,
    val packages: List<String>,
    val descEn: String,
    val descAr: String
) {
    val supportsShare: Boolean get() = packages.isNotEmpty()
    fun desc(arabic: Boolean): String = if (arabic) descAr else descEn
}

object ImageSearchEngines {
    const val LENS_ID = "google_lens"
    const val CHOOSER_ID = "other_apps"

    val all: List<SearchEngine> = listOf(
        SearchEngine(
            LENS_ID, "Google Lens", "https://lens.google.com/",
            listOf("com.google.ar.lens", "com.google.android.googlequicksearchbox"),
            "Identify objects, places and text, and find the image source",
            "تعرّف على الأشياء والأماكن والنصوص وابحث عن مصدر الصورة"
        ),
        SearchEngine(
            "yandex_images", "Yandex Images", "https://yandex.com/images/",
            listOf("ru.yandex.searchplugin", "ru.yandex.browser"),
            "Best at finding copies of an image and similar scenes",
            "الأفضل في إيجاد نسخ الصورة ومشاهد مشابهة"
        ),
        SearchEngine(
            "bing_visual", "Bing Visual Search", "https://www.bing.com/visualsearch",
            listOf("com.microsoft.bing"),
            "Visually similar images and shopping matches",
            "صور مشابهة بصريًا ونتائج تسوق"
        ),
        SearchEngine(
            "tineye", "TinEye", "https://tineye.com/",
            emptyList(),
            "Finds where an image first appeared and its edited versions",
            "يجد أين ظهرت الصورة أول مرة ونسخها المعدّلة"
        ),
        SearchEngine(
            "google_images", "Google Images", "https://images.google.com/imghp",
            emptyList(),
            "Classic reverse image search: tap the camera icon to upload",
            "بحث الصور العكسي الكلاسيكي: اضغط أيقونة الكاميرا للرفع"
        ),
        SearchEngine(
            "baidu_images", "Baidu Images", "https://graph.baidu.com/",
            emptyList(),
            "Strong for Asian websites, products and social media",
            "قوي في المواقع والمنتجات ووسائل التواصل الآسيوية"
        ),
        SearchEngine(
            "saucenao", "SauceNAO", "https://saucenao.com/",
            emptyList(),
            "Finds the source of anime, manga and artwork",
            "يجد مصدر الأنمي والمانجا والأعمال الفنية"
        ),
        SearchEngine(
            CHOOSER_ID, "Other apps…", "",
            listOf("*"),
            "Send the frame to any installed app that can search images",
            "أرسل الإطار إلى أي تطبيق مثبّت يمكنه البحث بالصور"
        )
    )

    fun byId(id: String): SearchEngine? = all.firstOrNull { it.id == id }
}
