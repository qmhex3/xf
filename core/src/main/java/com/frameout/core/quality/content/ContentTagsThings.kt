package com.frameout.core.quality.content

internal object ContentTagsThings {
    private val D = ContentGroup.Devices
    private val F = ContentGroup.Fashion
    private val P = ContentGroup.People

    val devices = listOf(
        tag("phone", D, "Phones", "هواتف", "Mobile phone"),
        tag("computer", D, "Computers", "حواسيب", "Computer"),
        tag("tv", D, "Televisions", "تلفاز", "Television"),
        tag("instrument", D, "Instruments", "آلات موسيقية", "Musical instrument", "Piano"),
        tag("clock", D, "Clocks", "ساعات", "Clock"),
        tag("bag", D, "Bags", "حقائب", "Bag", "Handbag"),
        tag("glasses", D, "Glasses", "نظارات", "Glasses", "Sunglasses", "Goggles"),
        tag("toy", D, "Toys", "ألعاب", "Toy", "Stuffed toy", "Plush", "Lego")
    )

    val fashion = listOf(
        tag("shoes", F, "Shoes", "أحذية", "Shoe", "Sneakers"),
        tag("hat", F, "Hats", "قبعات", "Hat", "Cap", "Beanie"),
        tag("dress", F, "Dresses", "فساتين", "Dress", "Gown", "Sari"),
        tag("clothes", F, "Clothes", "ملابس", "Jacket", "Jeans", "Jersey", "Outerwear", "Shorts", "Swimwear", "Tuxedo", "Blazer", "Leggings", "Scarf", "Denim", "Polo", "Tights"),
        tag("jewellery", F, "Jewellery", "مجوهرات", "Jewellery", "Necklace", "Bracelet", "Ring", "Bangle")
    )

    val people = listOf(
        special("face", "Faces", "وجوه", ContentDetector.Face),
        special("smile", "Smiles", "ابتسامات", ContentDetector.Smile),
        special("group", "Groups of people", "مجموعات أشخاص", ContentDetector.Group),
        tag("baby", P, "Babies", "أطفال رضع", "Baby"),
        tag("crowd", P, "Crowds & teams", "حشود وفرق", "Crowd", "Team"),
        special("text", "Text", "نصوص", ContentDetector.Text),
        special("code", "QR & barcodes", "QR وباركود", ContentDetector.Code)
    )
}
