package com.frameout.core.quality

internal object TagTextSv {
    val text = TagText(
        labels = TagLabelsSv.MAP,
        fixed = mapOf(
            TagRules.FACE to "Ansikten", TagRules.GROUP to "Grupp",
            TagRules.SMILE to "Leende", TagRules.EYES_CLOSED to "Slutna ögon",
            TagRules.GOOD_FACE to "Lyckat ansikte", TagRules.BODY to "Hel person",
            TagRules.TEXT to "Innehåller text", TagRules.CODE to "Valfri kod",
            "b:contact" to "Kontakt", "b:email" to "E-post",
            "b:isbn" to "ISBN", "b:phone" to "Telefon",
            "b:product" to "Produktkod", "b:sms" to "SMS",
            "b:plain" to "Textkod", "b:url" to "Länk",
            "b:wifi" to "Wi-Fi", "b:geo" to "Position",
            "b:event" to "Evenemang", "b:license" to "Körkort",
            "o:fashion_good" to "Mode", "o:food" to "Mat",
            "o:home_good" to "Hushållsartiklar", "o:place" to "Platser",
            "o:plant" to "Växter"
        ),
        groups = mapOf(
            "People" to "Människor", "Things" to "Saker och miljöer", "Categories" to "Kategorier",
            "Text" to "Text", "Codes" to "Koder"
        ),
        families = mapOf(
            "vehicles" to "Fordon", "animals" to "Djur", "people" to "Människor och kropp",
            "fashion" to "Mode och accessoarer", "food" to "Mat och dryck", "nature" to "Natur och landskap",
            "plants" to "Växter och trädgårdar", "sky" to "Himmel och väder", "places" to "Byggnader och platser",
            "home" to "Hem och inredning", "devices" to "Enheter och skärmar", "documents" to "Dokument och tavlor",
            "music" to "Musik och dans", "sports" to "Sport och friluftsliv", "events" to "Fester och evenemang",
            "toys" to "Leksaker och figurer", "crafts" to "Konst och hantverk", "objects" to "Föremål och verktyg"
        ),
        contentTags = mapOf(
            "building" to "Byggnader", "skyscraper" to "Skyskrapor", "bridge" to "Broar",
            "tower" to "Torn", "mosque" to "Moskéer", "church" to "Kyrkor",
            "castle" to "Slott och palats", "landmark" to "Landmärken", "road" to "Vägar",
            "meal" to "Maträtter", "fastfood" to "Snabbmat", "sweets" to "Godis och sötsaker",
            "bread" to "Bröd", "fruit" to "Frukt", "vegetable" to "Grönsaker",
            "coffee" to "Kaffe", "drink" to "Drycker", "soccer" to "Fotboll",
            "swimming" to "Vattensport", "running" to "Löpning och tävlingar", "dance" to "Dans",
            "concert" to "Konserter", "party" to "Fester", "wedding" to "Bröllop",
            "fireworks" to "Fyrverkerier", "fire" to "Eld", "car" to "Bilar",
            "heavy" to "Skåpbilar och lastbilar", "bus" to "Bussar", "motorcycle" to "Motorcyklar",
            "bicycle" to "Cyklar", "train" to "Tåg", "plane" to "Flygplan",
            "helicopter" to "Helikoptrar", "boat" to "Båtar och fartyg", "dog" to "Hundar",
            "cat" to "Katter", "bird" to "Fåglar", "horse" to "Hästar",
            "cattle" to "Nötkreatur", "bear" to "Björnar", "reptile" to "Reptiler",
            "insect" to "Insekter", "mountain" to "Berg", "beach" to "Stränder",
            "desert" to "Öknar", "forest" to "Skogar", "water" to "Sjöar och floder",
            "waterfall" to "Vattenfall", "sunset" to "Solnedgångar", "flower" to "Blommor",
            "ice" to "Is och glaciärer", "storm" to "Oväder", "phone" to "Telefoner",
            "computer" to "Datorer", "tv" to "TV-apparater", "instrument" to "Musikinstrument",
            "clock" to "Klockor", "bag" to "Väskor", "glasses" to "Glasögon",
            "toy" to "Leksaker", "shoes" to "Skor", "hat" to "Hattar",
            "dress" to "Klänningar", "clothes" to "Kläder", "jewellery" to "Smycken",
            "face" to "Ansikten", "smile" to "Leenden", "group" to "Persongrupper",
            "baby" to "Bebisar", "crowd" to "Folkmassor och lag", "text" to "Text",
            "code" to "QR- och streckkoder"
        ),
        contentGroups = mapOf(
            "Vehicles" to "Fordon", "Animals" to "Djur", "Nature" to "Natur",
            "Places" to "Platser och byggnader", "Food" to "Mat och dryck", "Activities" to "Sport och evenemang",
            "Devices" to "Enheter och föremål", "Fashion" to "Mode", "People" to "Människor och text"
        ),
        engines = mapOf(
            "google_lens" to "Känner igen objekt, platser och text och hittar bildens ursprung",
            "yandex_images" to "Bäst på att hitta kopior av bilden och liknande scener",
            "bing_visual" to "Visuellt liknande bilder och produkter som går att köpa",
            "tineye" to "Hittar var bilden först dök upp, samt redigerade versioner",
            "google_images" to "Klassisk omvänd bildsökning: tryck på kameraikonen för att ladda upp bilden",
            "baidu_images" to "Mycket effektiv för asiatiska webbplatser, produkter och sociala nätverk",
            "saucenao" to "Hittar källan till en anime, en manga eller en illustration",
            "other_apps" to "Skicka bilden till valfri installerad app för bildsökning"
        ),
        compare = mapOf(
            "stack" to "Ovanpå varandra", "side" to "Sida vid sida",
            "slider" to "Reglage", "swap" to "Byt plats",
            "fit" to "Anpassa", "zoomIn" to "Zooma in",
            "zoomOut" to "Zooma ut", "resetZoom" to "Återställ zoom",
            "hint" to "Nyp för att zooma · dubbelknacka", "loadFailed" to "Det gick inte att läsa in bilden",
            "divider" to "Delare för jämförelse", "diff" to "Skillnad",
            "diffBusy" to "Beräknar skillnader…", "link" to "Koppla ihop zoom",
            "unlink" to "Oberoende zoom", "actualSize" to "Faktiska pixlar",
            "diffPercentPrefix" to "Ändrat: "
        )
    )
}
