package com.frameout.core.quality

enum class TagGroup { People, Things, Categories, Text, Codes }

object TagNames {
    fun group(tag: String): TagGroup = when {
        tag.startsWith("p:") -> TagGroup.People
        tag.startsWith("o:") -> TagGroup.Categories
        tag.startsWith("t:") -> TagGroup.Text
        tag.startsWith("b:") -> TagGroup.Codes
        else -> TagGroup.Things
    }

    fun groupTitle(group: TagGroup, lang: String): String {
        val arabic = lang == "ar"
        TagText.forLang(lang)?.groups?.get(group.name)?.let { return it }
        return when (group) {
            TagGroup.People -> if (arabic) "الأشخاص" else "People"
            TagGroup.Things -> if (arabic) "الأشياء والمشاهد" else "Things & scenes"
            TagGroup.Categories -> if (arabic) "الفئات" else "Categories"
            TagGroup.Text -> if (arabic) "النصوص" else "Text"
            TagGroup.Codes -> if (arabic) "الأكواد" else "Codes"
        }
    }

    fun display(tag: String, lang: String): String {
        val fixed = FIXED[tag]
        if (fixed != null) return fixedName(tag, fixed, lang)
        TagFamilies.title(tag, lang)?.let { return it }
        val name = tag.substringAfter(':')
        return when (lang) {
            "ar" -> TagLabelsAr.MAP[name] ?: LOWER_AR[name.lowercase()] ?: LABELS_AR[name] ?: name
            else -> TagText.forLang(lang)?.label(name) ?: name
        }
    }

    private fun fixedName(tag: String, fixed: Pair<String, String>, lang: String): String = when (lang) {
        "ar" -> fixed.second
        else -> TagText.forLang(lang)?.fixed?.get(tag) ?: fixed.first
    }

    private val LOWER_AR: Map<String, String> by lazy {
        HashMap<String, String>().also { out ->
            LABELS_AR.forEach { (k, v) -> out[k.lowercase()] = v }
            TagLabelsAr.MAP.forEach { (k, v) -> out[k.lowercase()] = v }
        }
    }

    private val FIXED: Map<String, Pair<String, String>> = mapOf(
        TagRules.FACE to ("Faces" to "وجوه"),
        TagRules.GROUP to ("Group" to "مجموعة أشخاص"),
        TagRules.SMILE to ("Smiling" to "ابتسامة"),
        TagRules.EYES_CLOSED to ("Eyes closed" to "عيون مغمضة"),
        TagRules.GOOD_FACE to ("Good face" to "وجه سليم"),
        TagRules.BODY to ("Body" to "جسم كامل"),
        TagRules.TEXT to ("Has text" to "يحتوي نصًا"),
        TagRules.CODE to ("Any code" to "أي كود"),
        "b:contact" to ("Contact" to "جهة اتصال"),
        "b:email" to ("Email" to "بريد إلكتروني"),
        "b:isbn" to ("ISBN" to "ISBN"),
        "b:phone" to ("Phone" to "هاتف"),
        "b:product" to ("Product code" to "رمز منتج"),
        "b:sms" to ("SMS" to "رسالة نصية"),
        "b:plain" to ("Plain text code" to "كود نصي"),
        "b:url" to ("Link" to "رابط"),
        "b:wifi" to ("Wi-Fi" to "واي فاي"),
        "b:geo" to ("Location" to "موقع"),
        "b:event" to ("Event" to "حدث"),
        "b:license" to ("License" to "رخصة"),
        "o:fashion_good" to ("Fashion" to "أزياء"),
        "o:food" to ("Food items" to "مأكولات"),
        "o:home_good" to ("Home goods" to "أغراض منزلية"),
        "o:place" to ("Places" to "أماكن"),
        "o:plant" to ("Plants" to "نباتات")
    )

    private val LABELS_AR: Map<String, String> = mapOf(
        "Car" to "سيارة", "Vehicle" to "مركبة", "Bus" to "حافلة", "Truck" to "شاحنة", "Motorcycle" to "دراجة نارية",
        "Bicycle" to "دراجة", "Bicycle wheel" to "عجلة دراجة", "Wheel" to "عجلة", "Tire" to "إطار", "Boat" to "قارب",
        "Ship" to "سفينة", "Airplane" to "طائرة", "Aircraft" to "مركبة جوية", "Train" to "قطار", "Road" to "طريق",
        "Street" to "شارع", "Bridge" to "جسر", "Building" to "مبنى", "House" to "منزل", "Tower" to "برج",
        "Skyscraper" to "ناطحة سحاب", "City" to "مدينة", "Urban area" to "منطقة حضرية", "Church" to "كنيسة",
        "Mosque" to "مسجد", "Monument" to "نصب تذكاري", "Window" to "نافذة", "Door" to "باب", "Room" to "غرفة",
        "Furniture" to "أثاث", "Table" to "طاولة", "Chair" to "كرسي", "Couch" to "أريكة", "Bed" to "سرير",
        "Kitchen" to "مطبخ", "Sky" to "سماء", "Cloud" to "غيمة", "Sun" to "شمس", "Sunset" to "غروب",
        "Sunrise" to "شروق", "Moon" to "قمر", "Night" to "ليل", "Sea" to "بحر", "Ocean" to "محيط",
        "Beach" to "شاطئ", "Water" to "ماء", "River" to "نهر", "Lake" to "بحيرة", "Waterfall" to "شلال",
        "Mountain" to "جبل", "Hill" to "تل", "Desert" to "صحراء", "Snow" to "ثلج", "Rain" to "مطر",
        "Forest" to "غابة", "Tree" to "شجرة", "Plant" to "نبات", "Flower" to "زهرة", "Grass" to "عشب",
        "Leaf" to "ورقة شجر", "Garden" to "حديقة", "Fruit" to "فاكهة", "Vegetable" to "خضار", "Food" to "طعام",
        "Dish" to "طبق", "Cuisine" to "مطبخ عالمي", "Meal" to "وجبة", "Drink" to "مشروب", "Coffee" to "قهوة",
        "Cake" to "كعكة", "Bread" to "خبز", "Pizza" to "بيتزا", "Dog" to "كلب", "Cat" to "قطة", "Pet" to "حيوان أليف",
        "Bird" to "طائر", "Fish" to "سمكة", "Horse" to "حصان", "Cow" to "بقرة", "Sheep" to "خروف", "Animal" to "حيوان",
        "Insect" to "حشرة", "Butterfly" to "فراشة", "Person" to "شخص", "People" to "أشخاص", "Crowd" to "حشد",
        "Face" to "وجه", "Smile" to "ابتسامة", "Selfie" to "سيلفي", "Baby" to "طفل رضيع", "Child" to "طفل",
        "Hair" to "شعر", "Beard" to "لحية", "Eye" to "عين", "Hand" to "يد", "Finger" to "إصبع", "Leg" to "ساق",
        "Arm" to "ذراع", "Foot" to "قدم", "Skin" to "بشرة", "Clothing" to "ملابس", "Footwear" to "أحذية",
        "Shoe" to "حذاء", "Hat" to "قبعة", "Glasses" to "نظارات", "Jeans" to "جينز", "Dress" to "فستان",
        "Fashion" to "موضة", "Bag" to "حقيبة", "Watch" to "ساعة", "Jewellery" to "مجوهرات", "Sports" to "رياضة",
        "Football" to "كرة قدم", "Ball" to "كرة", "Stadium" to "ملعب", "Team" to "فريق", "Music" to "موسيقى",
        "Guitar" to "غيتار", "Concert" to "حفل موسيقي", "Dance" to "رقص", "Party" to "حفلة", "Wedding" to "زفاف",
        "Fun" to "مرح", "Event" to "حدث", "Fireworks" to "ألعاب نارية", "Toy" to "لعبة", "Phone" to "هاتف",
        "Mobile phone" to "هاتف محمول", "Computer" to "حاسوب", "Laptop" to "حاسوب محمول", "Television" to "تلفاز",
        "Screen" to "شاشة", "Camera" to "كاميرا", "Book" to "كتاب", "Paper" to "ورق", "Text" to "نص",
        "Art" to "فن", "Painting" to "لوحة", "Drawing" to "رسم", "Cartoon" to "كرتون", "Sculpture" to "منحوتة",
        "Product" to "منتج", "Tool" to "أداة", "Machine" to "آلة", "Technology" to "تقنية", "Light" to "ضوء",
        "Fire" to "نار", "Smoke" to "دخان", "Flag" to "علم", "Sign" to "لافتة", "Poster" to "ملصق",
        "Crowd" to "حشد", "Soldier" to "جندي", "Military" to "عسكري", "Police" to "شرطة", "Doctor" to "طبيب",
        "Hospital" to "مستشفى", "School" to "مدرسة", "Market" to "سوق", "Shop" to "متجر", "Restaurant" to "مطعم",
        "Park" to "حديقة عامة", "Playground" to "ملعب أطفال", "Pool" to "مسبح", "Swimming" to "سباحة",
        "Cycling" to "ركوب الدراجة", "Running" to "ركض", "Travel" to "سفر", "Tourism" to "سياحة", "Landscape" to "منظر طبيعي"
    )
}
