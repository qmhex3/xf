package com.frameout.core.quality

internal object TagTextFa {
    val text = TagText(
        labels = TagLabelsFa.MAP,
        fixed = mapOf(
            TagRules.FACE to "چهره‌ها", TagRules.GROUP to "گروه",
            TagRules.SMILE to "لبخند", TagRules.EYES_CLOSED to "چشم‌های بسته",
            TagRules.GOOD_FACE to "چهره خوب", TagRules.BODY to "بدن",
            TagRules.TEXT to "دارای متن", TagRules.CODE to "هر کدی",
            "b:contact" to "مخاطب", "b:email" to "ایمیل",
            "b:isbn" to "ISBN", "b:phone" to "تلفن",
            "b:product" to "کد محصول", "b:sms" to "SMS",
            "b:plain" to "کد متنی", "b:url" to "پیوند",
            "b:wifi" to "Wi-Fi", "b:geo" to "مکان",
            "b:event" to "رویداد", "b:license" to "گواهینامه",
            "o:fashion_good" to "مد", "o:food" to "مواد غذایی",
            "o:home_good" to "لوازم خانه", "o:place" to "مکان‌ها",
            "o:plant" to "گیاهان"
        ),
        groups = mapOf(
            "People" to "افراد", "Things" to "اشیا و صحنه‌ها", "Categories" to "دسته‌ها",
            "Text" to "متن", "Codes" to "کدها"
        ),
        families = mapOf(
            "vehicles" to "وسایل نقلیه", "animals" to "حیوانات", "people" to "افراد و بدن",
            "fashion" to "مد و اکسسوری", "food" to "غذا و نوشیدنی", "nature" to "طبیعت و منظره",
            "plants" to "گیاهان و باغ‌ها", "sky" to "آسمان و آب‌وهوا", "places" to "ساختمان‌ها و مکان‌ها",
            "home" to "خانه و دکوراسیون", "devices" to "دستگاه‌ها و صفحه‌ها", "documents" to "اسناد و تخته‌ها",
            "music" to "موسیقی و رقص", "sports" to "ورزش و فعالیت در فضای باز", "events" to "جشن‌ها و رویدادها",
            "toys" to "اسباب‌بازی و شخصیت‌ها", "crafts" to "هنر و صنایع دستی", "objects" to "اشیا و ابزارها"
        ),
        contentTags = mapOf(
            "building" to "ساختمان‌ها", "skyscraper" to "آسمان‌خراش‌ها", "bridge" to "پل‌ها",
            "tower" to "برج‌ها", "mosque" to "مسجدها", "church" to "کلیساها",
            "castle" to "قلعه‌ها و کاخ‌ها", "landmark" to "جاذبه‌های دیدنی", "road" to "جاده‌ها",
            "meal" to "غذاها", "fastfood" to "فست‌فود", "sweets" to "شیرینی‌ها",
            "bread" to "نان", "fruit" to "میوه‌ها", "vegetable" to "سبزی‌ها",
            "coffee" to "قهوه", "drink" to "نوشیدنی‌ها", "soccer" to "فوتبال",
            "swimming" to "ورزش‌های آبی", "running" to "دویدن و مسابقه", "dance" to "رقص",
            "concert" to "کنسرت‌ها", "party" to "مهمانی‌ها", "wedding" to "عروسی‌ها",
            "fireworks" to "آتش‌بازی", "fire" to "آتش", "car" to "خودروها",
            "heavy" to "ون و کامیون", "bus" to "اتوبوس‌ها", "motorcycle" to "موتورسیکلت‌ها",
            "bicycle" to "دوچرخه‌ها", "train" to "قطارها", "plane" to "هواپیماها",
            "helicopter" to "هلیکوپترها", "boat" to "قایق‌ها و کشتی‌ها", "dog" to "سگ‌ها",
            "cat" to "گربه‌ها", "bird" to "پرندگان", "horse" to "اسب‌ها",
            "cattle" to "گاو و گوسفند", "bear" to "خرس‌ها", "reptile" to "خزندگان",
            "insect" to "حشرات", "mountain" to "کوه‌ها", "beach" to "ساحل‌ها",
            "desert" to "بیابان", "forest" to "جنگل‌ها", "water" to "دریاچه‌ها و رودخانه‌ها",
            "waterfall" to "آبشارها", "sunset" to "غروب‌ها", "flower" to "گل‌ها",
            "ice" to "یخ و یخچال‌ها", "storm" to "طوفان‌ها", "phone" to "تلفن‌ها",
            "computer" to "رایانه‌ها", "tv" to "تلویزیون‌ها", "instrument" to "سازهای موسیقی",
            "clock" to "ساعت‌ها", "bag" to "کیف‌ها", "glasses" to "عینک‌ها",
            "toy" to "اسباب‌بازی‌ها", "shoes" to "کفش‌ها", "hat" to "کلاه‌ها",
            "dress" to "پیراهن‌ها", "clothes" to "لباس‌ها", "jewellery" to "جواهرات",
            "face" to "چهره‌ها", "smile" to "لبخندها", "group" to "گروه‌های افراد",
            "baby" to "نوزادان", "crowd" to "جمعیت و تیم‌ها", "text" to "متن",
            "code" to "QR و بارکد"
        ),
        contentGroups = mapOf(
            "Vehicles" to "وسایل نقلیه", "Animals" to "حیوانات", "Nature" to "طبیعت",
            "Places" to "مکان‌ها و ساختمان‌ها", "Food" to "غذا و نوشیدنی", "Activities" to "ورزش و رویدادها",
            "Devices" to "دستگاه‌ها و اشیا", "Fashion" to "مد", "People" to "افراد و متن"
        ),
        engines = mapOf(
            "google_lens" to "اشیا، مکان‌ها و متن را تشخیص می‌دهد و منبع تصویر را پیدا می‌کند",
            "yandex_images" to "بهترین گزینه برای یافتن نسخه‌های تکراری تصویر و صحنه‌های مشابه",
            "bing_visual" to "تصاویر مشابه از نظر ظاهری و محصولات قابل خرید",
            "tineye" to "پیدا می‌کند تصویر اولین بار کجا منتشر شده و نسخه‌های ویرایش‌شده آن را نشان می‌دهد",
            "google_images" to "جست‌وجوی معکوس کلاسیک تصویر: برای بارگذاری روی نماد دوربین بزنید",
            "baidu_images" to "مناسب سایت‌های آسیایی، محصولات و شبکه‌های اجتماعی",
            "saucenao" to "منبع انیمه، مانگا و آثار هنری را پیدا می‌کند",
            "other_apps" to "فریم را به هر برنامه جست‌وجوی تصویر نصب‌شده بفرستید"
        ),
        compare = mapOf(
            "stack" to "ستونی", "side" to "کنار هم",
            "slider" to "لغزنده", "swap" to "جابه‌جایی",
            "fit" to "متناسب", "zoomIn" to "بزرگ‌نمایی",
            "zoomOut" to "کوچک‌نمایی", "resetZoom" to "بازنشانی بزرگ‌نمایی",
            "hint" to "برای بزرگ‌نمایی دو انگشت را حرکت دهید · دو بار ضربه بزنید", "loadFailed" to "بارگذاری تصویر ناموفق بود",
            "divider" to "جداکننده مقایسه", "diff" to "تفاوت",
            "diffBusy" to "در حال محاسبه تفاوت‌ها…", "link" to "اتصال بزرگ‌نمایی",
            "unlink" to "بزرگ‌نمایی مستقل", "actualSize" to "پیکسل‌های واقعی",
            "diffPercentPrefix" to "تغییریافته: "
        )
    )
}
