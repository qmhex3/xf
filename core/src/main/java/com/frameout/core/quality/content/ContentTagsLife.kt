package com.frameout.core.quality.content

internal object ContentTagsLife {
    private val P = ContentGroup.Places
    private val F = ContentGroup.Food
    private val S = ContentGroup.Activities

    val places = listOf(
        tag("building", P, "Buildings", "مبانٍ", "Building"),
        tag("skyscraper", P, "Skyscrapers", "ناطحات سحاب", "Skyscraper", "Skyline"),
        tag("bridge", P, "Bridges", "جسور", "Bridge"),
        tag("tower", P, "Towers", "أبراج", "Tower", "Lighthouse"),
        tag("mosque", P, "Mosques", "مساجد", "Mosque"),
        tag("church", P, "Churches", "كنائس", "Church", "Cathedral"),
        tag("castle", P, "Castles & palaces", "قلاع وقصور", "Castle", "Palace"),
        tag("landmark", P, "Landmarks", "معالم وآثار", "Monument", "Statue", "Ruins", "Temple"),
        tag("road", P, "Roads", "طرق", "Road", "Asphalt")
    )

    val food = listOf(
        tag("meal", F, "Meals", "وجبات", "Food", "Cuisine", "Meal", "Lunch", "Supper", "Bento", "Couscous"),
        tag("fastfood", F, "Fast food", "وجبات سريعة", "Pizza", "Cheeseburger", "Hot dog", "Fast food"),
        tag("sweets", F, "Sweets", "حلويات", "Cake", "Cookie", "Pie", "Icing", "Gelato"),
        tag("bread", F, "Bread", "خبز", "Bread"),
        tag("fruit", F, "Fruit", "فواكه", "Fruit"),
        tag("vegetable", F, "Vegetables", "خضار", "Vegetable"),
        tag("coffee", F, "Coffee", "قهوة", "Coffee", "Cappuccino"),
        tag("drink", F, "Drinks", "مشروبات", "Juice", "Wine", "Alcohol", "Cola")
    )

    val activities = listOf(
        tag("soccer", S, "Football", "كرة قدم وملاعب", "Soccer", "Stadium"),
        tag("swimming", S, "Water sports", "سباحة ورياضات مائية", "Swimming", "Pool", "Surfing", "Scuba diving", "Snorkeling", "Windsurfing", "Waterskiing", "Wakeboarding"),
        tag("running", S, "Running & races", "جري وسباق", "Running", "Marathon", "Race", "Track"),
        tag("dance", S, "Dancing", "رقص", "Dance", "Ballroom"),
        tag("concert", S, "Concerts", "حفلات موسيقية", "Concert", "Nightclub", "Musician", "Singer", "Deejay"),
        tag("party", S, "Parties", "احتفالات", "Party", "Balloon", "Prom", "Graduation", "Carnival"),
        tag("wedding", S, "Weddings", "زفاف", "Bride", "Groom", "Marriage", "Veil"),
        tag("fireworks", S, "Fireworks", "ألعاب نارية", "Fireworks", "Sparkler"),
        tag("fire", S, "Fire", "نار", "Fire", "Bonfire")
    )
}
